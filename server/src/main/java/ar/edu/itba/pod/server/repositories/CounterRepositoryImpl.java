package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.exceptions.*;
import ar.edu.itba.pod.server.models.*;
import ar.edu.itba.pod.server.utils.Pair;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CounterRepositoryImpl implements CounterRepository {

    private final Map<String, Queue<Assignment>> assignmentQueue = new HashMap<>();
    private final Map<String, TreeSet<CountersRange>> sectorCounters = new HashMap<>();
    private final Set<String> assignedFlights = new HashSet<>();
    private final Map<Range, Queue<String>> passengerCounters = new HashMap<>();
    private final ReadWriteLock sectorCountersLock = new ReentrantReadWriteLock(true);
    private final ReadWriteLock assignmentQueueLock = new ReentrantReadWriteLock(true);
    private final ReadWriteLock assignedFlightsLock = new ReentrantReadWriteLock(true);
    private final ReadWriteLock passengerCountersLock = new ReentrantReadWriteLock(true);
    int lastCounter = 0;

    @Override
    public void addSector(String sector) throws AlreadyExistsException {
        sectorCountersLock.readLock().lock();
        try {
            if (sectorCounters.containsKey(sector)) {
                throw new AlreadyExistsException(sector);
            }
        } finally {
            sectorCountersLock.readLock().unlock();
        }
        sectorCountersLock.writeLock().lock();
        try {
            sectorCounters.put(sector, new TreeSet<>());
        } finally {
            sectorCountersLock.writeLock().unlock();
        }
    }

    @Override
    public boolean hasSector(String sector) {
        boolean result;
        sectorCountersLock.readLock().lock();
        try {
            result = sectorCounters.containsKey(sector);
        } finally {
            sectorCountersLock.readLock().unlock();
        }
        return result;
    }

    @Override
    public List<Sector> getSectors() {
        List<Sector> sectors = new ArrayList<>();
        sectorCountersLock.readLock().lock();
        try {
            sectorCounters.forEach((k, v) -> sectors.add(new Sector(k, v.stream().toList())));
        } finally {
            sectorCountersLock.readLock().unlock();
        }
        return sectors;
    }

    @Override
    public Optional<Sector> getSector(String sectorName) {
        sectorCountersLock.readLock().lock();
        Optional<Sector> result = Optional.empty();
        try {
            if (sectorCounters.containsKey(sectorName)) {
                result = Optional.of(new Sector(sectorName, sectorCounters.get(sectorName).stream().toList()));
            }
        } finally {
            sectorCountersLock.readLock().unlock();
        }
        return result;
    }

    private Range assignInfoToAvailableCounterRange(Assignment counterAssignment, CountersRange freeRange, TreeSet<CountersRange> set) {
        int assignedFrom = freeRange.range().from();
        int assignedTo = assignedFrom + counterAssignment.counterCount() - 1;

        int remainingFrom = assignedTo + 1;
        int remainingTo = freeRange.range().to();

        CountersRange assignedRange =
                new CountersRange(
                        new Range(assignedFrom, assignedTo),
                        new AssignedInfo(
                                counterAssignment.airline(),
                                counterAssignment.flights(),
                                0)
                );
        set.remove(freeRange);
        set.add(assignedRange);
        if (remainingFrom <= remainingTo) {
            CountersRange remainingRange =
                    new CountersRange(
                            new Range(remainingFrom, remainingTo)
                    );
            set.add(remainingRange);
        }
        return assignedRange.range();
    }

    private void tryPendingAssignments(String sectorName) {
        List<String> newlyAssignedFlights = new ArrayList<>();
        assignmentQueueLock.writeLock().lock();
        while (!sectorCountersLock.writeLock().tryLock()) {
            assignmentQueueLock.writeLock().unlock();
            assignmentQueueLock.writeLock().lock();
        }
        try {
            if (!assignmentQueue.containsKey(sectorName)) {
                return;
            }
            List<Assignment> assignments = (List<Assignment>) assignmentQueue.get(sectorName);
            if (assignments.isEmpty()) {
                return;
            }
            int assignmentsSize = assignments.size();
            List<Integer> toRemove = new ArrayList<>();
            for (int i = 0; i < assignmentsSize; i++) {
                Assignment assignment = assignments.get(i);
                TreeSet<CountersRange> set = sectorCounters.get(sectorName);
                Optional<CountersRange> maybeFreeCounterRange =
                        set.stream().filter(
                                range -> range.assignedInfo().isEmpty() && (range.range().to() - range.range().from() + 1) >= assignment.counterCount()
                        ).findFirst();

                // Si el primero no se pudo asignar, probamos con el siguiente
                if (maybeFreeCounterRange.isEmpty()) {
                    continue;
                }

                Range assignedRange = assignInfoToAvailableCounterRange(assignment, maybeFreeCounterRange.get(), set);
                newlyAssignedFlights.addAll(assignment.flights());

                passengerCountersLock.writeLock().lock();
                try {
                    passengerCounters.putIfAbsent(assignedRange, new LinkedList<>());
                } finally {
                    passengerCountersLock.writeLock().unlock();
                }

                toRemove.add(i);
                assignment.getOnAssigned().accept(assignedRange);

                int pendingAhead = 0;

                for (int j = 0; j < assignments.size(); j++) {
                    Assignment pendingAssignment = assignments.get(j);

                    if (toRemove.contains(j)) {
                        continue;
                    }

                    pendingAssignment.getOnMoved().accept(pendingAhead);
                    pendingAhead++;
                }
            }

            // Remove the assigned assignments
            for (int i = toRemove.size() - 1; i >= 0; i--) {
                assignments.remove(toRemove.get(i).intValue());
            }


        } finally {
            assignmentQueueLock.writeLock().unlock();
            sectorCountersLock.writeLock().unlock();
        }

        assignmentQueueLock.writeLock().lock();
        try {
            assignedFlights.addAll(newlyAssignedFlights);
        } finally {
            assignmentQueueLock.writeLock().unlock();
        }
    }

    @Override
    public Range addCounters(String sector, int counterCount) throws NoSuchElementException {
        sectorCountersLock.readLock().lock();
        try {
            if (!sectorCounters.containsKey(sector)) {
                throw new NoSuchElementException("Sector does not exist");
            }
        } finally {
            sectorCountersLock.readLock().unlock();
        }
        sectorCountersLock.writeLock().lock();
        try {
            TreeSet<CountersRange> set = sectorCounters.get(sector);
            CountersRange newRange;
            Optional<CountersRange> maybeLastCounter = set.stream().filter(range -> range.range().to() == lastCounter).findFirst();
            // si no es el ultimo o si tiene assignedInfo
            //   -> creo uno nuevo
            if (maybeLastCounter.isEmpty() || maybeLastCounter.get().assignedInfo().isPresent()) {
                newRange = new CountersRange(new Range(lastCounter + 1, lastCounter + counterCount));
            } else {
                CountersRange lastCounter = maybeLastCounter.get();
                set.remove(lastCounter);
                newRange = new CountersRange(new Range(lastCounter.range().from(), this.lastCounter + counterCount));
            }
            set.add(newRange);
            lastCounter = lastCounter + counterCount;
        } finally {
            sectorCountersLock.writeLock().unlock();
        }
        tryPendingAssignments(sector);
        return new Range(lastCounter - counterCount + 1, lastCounter);
    }

    @Override
    public boolean hasCounters() {
        sectorCountersLock.readLock().lock();
        try {
            for (TreeSet<CountersRange> counterRanges : sectorCounters.values()) {
                if (!counterRanges.isEmpty()) {
                    return true;
                }
            }
        } finally {
            sectorCountersLock.readLock().unlock();
        }
        return false;
    }

    @Override
    public Optional<CountersRange> getFlightCounters(String flight) {
        sectorCountersLock.readLock().lock();
        try {
            for (TreeSet<CountersRange> set : sectorCounters.values()) {
                for (CountersRange range : set) {
                    if (range.assignedInfo().isPresent()
                            && range.assignedInfo().get().flights().contains(flight)) {
                        return Optional.of(range);
                    }
                }
            }
        } finally {
            sectorCountersLock.readLock().unlock();
        }
        return Optional.empty();
    }

    @Override
    public Optional<Pair<CountersRange, String>> getFlightCountersAndSector(String flight) {
        sectorCountersLock.readLock().lock();
        try {
            for (Map.Entry<String, TreeSet<CountersRange>> entry : sectorCounters.entrySet()) {
                for (CountersRange range : entry.getValue()) {
                    if (range.assignedInfo().isPresent()
                            && range.assignedInfo().get().flights().contains(flight)) {
                        return Optional.of(new Pair<>(range, entry.getKey()));
                    }
                }
            }
        } finally {
            sectorCountersLock.readLock().unlock();
        }
        return Optional.empty();
    }


    private boolean isQueued(String sectorName, Assignment assignment) {
        assignmentQueueLock.readLock().lock();
        try {
            Queue<Assignment> queue = assignmentQueue.get(sectorName);
            if (queue == null || !queue.contains(assignment)) {
                return false;
            }
        } finally {
            assignmentQueueLock.readLock().unlock();
        }
        return true;
    }

    @Override
    public Pair<Range, Integer> assignCounterAssignment(String sectorName, Assignment counterAssignment) throws FlightAlreadyAssignedException, FlightAlreadyQueuedException, FlightAlreadyCheckedInException {
        if (!hasSector(sectorName)) {
            throw new NoSuchElementException("Sector does not exist");
        }

        boolean hasFlightAssigned;
        sectorCountersLock.readLock().lock();
        try {
            TreeSet<CountersRange> set = sectorCounters.get(sectorName);

            // Check if there is a flight from the CounterAssignment
            // that is already assigned to an existing CountersRange
            hasFlightAssigned =
                    set.stream()
                            .filter(countersRange -> countersRange.assignedInfo().isPresent())
                            .anyMatch(
                                    countersRange ->
                                            counterAssignment.flights().stream()
                                                    .anyMatch(
                                                            countersRange.assignedInfo().get().flights()
                                                                    ::contains));
        } finally {
            sectorCountersLock.readLock().unlock();
        }


        if (hasFlightAssigned) {
            throw new FlightAlreadyAssignedException("Flight already assigned to a counter");
        }

        if (isQueued(sectorName, counterAssignment)) {
            throw new FlightAlreadyQueuedException("Flight already queued");
        }
        assignedFlightsLock.readLock().lock();
        try {
            if (counterAssignment.flights().stream().anyMatch(assignedFlights::contains)) {
                throw new FlightAlreadyCheckedInException("Flight already checked in");
            }
        } finally {
            assignedFlightsLock.readLock().unlock();
        }

        sectorCountersLock.writeLock().lock();
        while (!assignedFlightsLock.writeLock().tryLock()) {
            sectorCountersLock.writeLock().unlock();
            sectorCountersLock.writeLock().lock();
        }
        Range range;
        try {
            TreeSet<CountersRange> set = sectorCounters.get(sectorName);
            Optional<CountersRange> maybeFreeCounterRange =
                    set.stream().filter(
                            r -> r.assignedInfo().isEmpty() && (r.range().to() - r.range().from() + 1) >= counterAssignment.counterCount()
                    ).findFirst();
            if (maybeFreeCounterRange.isEmpty()) {
                int pending = addAssignmentToQueue(sectorName, counterAssignment);
                return new Pair<>(null, pending);
            }
            range = assignInfoToAvailableCounterRange(counterAssignment, maybeFreeCounterRange.get(), set);
            assignedFlights.addAll(counterAssignment.flights());
        } finally {
            assignedFlightsLock.writeLock().unlock();
            sectorCountersLock.writeLock().unlock();
        }

        passengerCountersLock.writeLock().lock();
        try {
            passengerCounters.putIfAbsent(range, new LinkedList<>());
        } finally {
            passengerCountersLock.writeLock().unlock();
        }

        return new Pair<>(range, 0);

    }

    private int addAssignmentToQueue(String sectorName, Assignment counterAssignment) {
        assignmentQueueLock.writeLock().lock();
        try {
            assignmentQueue.putIfAbsent(sectorName, new LinkedList<>());
            Queue<Assignment> queue = assignmentQueue.get(sectorName);
            int pending = queue.size();
            queue.add(counterAssignment);
            return pending;
        } finally {
            assignmentQueueLock.writeLock().unlock();
        }
    }

    @Override
    public List<String> getPreviouslyAssignedFlights() {
        assignedFlightsLock.readLock().lock();
        try {
            return assignedFlights.stream().toList();
        } finally {
            assignedFlightsLock.readLock().unlock();
        }
    }

    @Override
    public CountersRange freeCounters(String sectorName, int counterFrom, String airline) throws NoSuchElementException, HasPendingPassengersException, UnauthorizedException {
        if (!hasSector(sectorName)) {
            throw new NoSuchElementException("Sector does not exist");
        }
        Optional<CountersRange> maybeToFreeCounterRange;
        sectorCountersLock.readLock().lock();
        try {
            TreeSet<CountersRange> set = sectorCounters.get(sectorName);
            maybeToFreeCounterRange =
                    set.stream().filter(
                            range -> range.range().from() == counterFrom && range.assignedInfo().isPresent()
                    ).findFirst();
            if (maybeToFreeCounterRange.isEmpty() || maybeToFreeCounterRange.get().assignedInfo().isEmpty()) {
                throw new NoSuchElementException("Counter does not exist or not assigned");
            }

            if (!maybeToFreeCounterRange.get().assignedInfo().get().airline().equals(airline)) {
                throw new UnauthorizedException("Counter is not assigned to the airline");
            }

        } finally {
            sectorCountersLock.readLock().unlock();
        }

        passengerCountersLock.readLock().lock();
        try {
            if (!passengerCounters.getOrDefault(maybeToFreeCounterRange.get().range(), new LinkedList<>()).isEmpty()) {
                throw new HasPendingPassengersException("Counter has pending passengers");
            }
        } finally {
            passengerCountersLock.readLock().unlock();
        }
        CountersRange toFree;
        sectorCountersLock.writeLock().lock();
        try {
            TreeSet<CountersRange> set = sectorCounters.get(sectorName);
            toFree = maybeToFreeCounterRange.get();

            set.remove(toFree);
            int newFrom = toFree.range().from();
            int newTo = toFree.range().to();

            Optional<CountersRange> maybeBefore =
                    set.stream().filter(
                            range -> range.range().to() == (toFree.range().from() - 1) && range.assignedInfo().isEmpty()
                    ).findFirst();
            Optional<CountersRange> maybeAfter =
                    set.stream().filter(
                            range -> range.range().from() == (toFree.range().to() + 1) && range.assignedInfo().isEmpty()
                    ).findFirst();
            if (maybeBefore.isPresent()) {
                set.remove(maybeBefore.get());
                newFrom = maybeBefore.get().range().from();
            }
            if (maybeAfter.isPresent()) {
                set.remove(maybeAfter.get());
                newTo = maybeAfter.get().range().to();
            }
            set.add(new CountersRange(new Range(newFrom, newTo)));
        } finally {
            sectorCountersLock.writeLock().unlock();
        }
        tryPendingAssignments(sectorName);
        return toFree;
    }

    @Override
    public Queue<Assignment> getQueuedAssignments(String sector) {
        assignedFlightsLock.readLock().lock();
        try {
            return assignmentQueue.getOrDefault(sector, new LinkedList<>());
        } finally {
            assignedFlightsLock.readLock().unlock();
        }
    }

    @Override
    public boolean hasPassengerInCounter(Range counterRange, String booking) {
        passengerCountersLock.readLock().lock();
        try {
            return passengerCounters.getOrDefault(counterRange, new LinkedList<>()).contains(booking);
        } finally {
            passengerCountersLock.readLock().unlock();
        }
    }

    @Override
    public int addPassengerToQueue(Range range, String booking) throws AlreadyExistsException, NoSuchElementException {
        if (hasPassengerInCounter(range, booking)) {
            throw new AlreadyExistsException("Passenger already in queue");
        }
        passengerCountersLock.writeLock().lock();
        while (!sectorCountersLock.writeLock().tryLock()) {
            passengerCountersLock.writeLock().unlock();
            passengerCountersLock.writeLock().lock();
        }
        try {
            Queue<String> passengers = passengerCounters.getOrDefault(range, null);
            if (passengers == null) {
                throw new NoSuchElementException("Counter does not exist");
            }
            passengers.add(booking);

            CountersRange counterRange = null;
            TreeSet<CountersRange> set = new TreeSet<>();
            for (TreeSet<CountersRange> setAux : sectorCounters.values()) {
                for (CountersRange rangeAux : setAux) {
                    if (rangeAux.range().equals(range)) {
                        counterRange = rangeAux;
                        set = setAux;
                        break;
                    }
                }
            }
            if (set.isEmpty()) {
                throw new NoSuchElementException("Counter does not exist");
            }

            set.remove(counterRange);
            AssignedInfo assignedInfo = new AssignedInfo(
                    counterRange.assignedInfo().orElseThrow().airline(),
                    counterRange.assignedInfo().orElseThrow().flights(),
                    passengers.size()
            );
            set.add(new CountersRange(counterRange.range(), assignedInfo));

            return passengers.size();
        } finally {
            sectorCountersLock.writeLock().unlock();
            passengerCountersLock.writeLock().unlock();
        }
    }

    @Override
    public List<Optional<String>> checkinCounters(String sector, int counterFrom, String airline) throws NoSuchElementException, UnauthorizedException {
        if (!hasSector(sector)) {
            throw new NoSuchElementException("Sector does not exist");
        }

        sectorCountersLock.readLock().lock();
        while (!passengerCountersLock.writeLock().tryLock()) {
            sectorCountersLock.readLock().unlock();
            sectorCountersLock.readLock().lock();
        }
        try {
            TreeSet<CountersRange> set = sectorCounters.get(sector);
            Optional<CountersRange> maybeCounter =
                    set.stream().filter(
                            range -> range.range().from() == counterFrom
                    ).findFirst();
            if (maybeCounter.isEmpty() || maybeCounter.get().assignedInfo().isEmpty()) {
                throw new NoSuchElementException("Counter does not exist or not assigned");
            }

            if (!maybeCounter.get().assignedInfo().get().airline().equals(airline)) {
                throw new UnauthorizedException("Counter is not assigned to the airline");
            }

            CountersRange counterRange = maybeCounter.get();

            List<Optional<String>> result = new ArrayList<>();
            Queue<String> passengers = passengerCounters.getOrDefault(counterRange.range(), new LinkedList<>());

            for (int i = 0; i < counterRange.range().to() - counterRange.range().from() + 1; i++) {
                result.add(Optional.ofNullable(passengers.poll()));
            }

            set.remove(counterRange);
            AssignedInfo assignedInfo = new AssignedInfo(
                    counterRange.assignedInfo().orElseThrow().airline(),
                    counterRange.assignedInfo().orElseThrow().flights(),
                    passengers.size()
            );
            set.add(new CountersRange(counterRange.range(), assignedInfo));
            return result;
        } finally {
            passengerCountersLock.writeLock().unlock();
            sectorCountersLock.readLock().unlock();
        }
    }
}