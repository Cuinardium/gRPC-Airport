package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.exceptions.*;
import ar.edu.itba.pod.server.models.*;
import ar.edu.itba.pod.server.utils.Pair;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CounterRepositoryImpl implements CounterRepository {

    int lastCounter = 0;
    private final Map<String, Queue<Assignment>> assigmentQueue = new HashMap<>();
    private final Map<String, Set<CountersRange>> sectorCounters = new HashMap<>();
    private final Set<String> assignedFlights = new HashSet<>();

    private final ReadWriteLock sectorCountersLock = new ReentrantReadWriteLock(true);
    private final ReadWriteLock assignmentQueueLock = new ReentrantReadWriteLock(true);
    private final ReadWriteLock assignedFlightsLock = new ReentrantReadWriteLock(true);

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
            sectorCounters.put(sector, new HashSet<>());
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
            sectorCounters.forEach((k, v)->  sectors.add(new Sector(k, v.stream().toList())));
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

    private Range assignInfoToAvailableCounterRange(Assignment counterAssignment, CountersRange freeRange, Set<CountersRange> set) {
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
        if(remainingFrom <= remainingTo) {
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
        // TODO: revisar esto
        while(!sectorCountersLock.writeLock().tryLock()) {
            assignmentQueueLock.writeLock().unlock();
            assignmentQueueLock.writeLock().lock();
        }
        try {
            if (!assigmentQueue.containsKey(sectorName)) {
                return;
            }
            Queue<Assignment> assignments = assigmentQueue.get(sectorName);
            if (assignments.isEmpty()) {
                return;
            }
            for(int i = 0; i < assignments.size(); i++){
                Assignment assignment = assignments.peek();
                Set<CountersRange> set = sectorCounters.get(sectorName);
                Optional<CountersRange> maybeFreeCounterRange =
                        set.stream().filter(
                                range -> range.assignedInfo().isEmpty() && (range.range().to() - range.range().from() + 1) >= assignment.counterCount()
                        ).findFirst();

                // Es incremental asi que si alguno no se pudo asignar -> el resto tampoco se va a poder
                if (maybeFreeCounterRange.isEmpty()){
                    break;
                }

                assignInfoToAvailableCounterRange(assignment, maybeFreeCounterRange.get(), set);
                newlyAssignedFlights.addAll(assignment.flights());
                assignments.poll();
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
            Set<CountersRange> set = sectorCounters.get(sector);
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
            for(Set<CountersRange> counterRanges : sectorCounters.values()) {
                if(!counterRanges.isEmpty()) {
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
            for (Set<CountersRange> set : sectorCounters.values()) {
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
            for(Map.Entry<String, Set<CountersRange>> entry : sectorCounters.entrySet()) {
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
            Queue<Assignment> queue = assigmentQueue.get(sectorName);
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
        if(!hasSector(sectorName)) {
            throw new NoSuchElementException("Sector does not exist");
        }

        boolean hasFlightAssigned;
        sectorCountersLock.readLock().lock();
        try {
            Set<CountersRange> set = sectorCounters.get(sectorName);

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


        if(hasFlightAssigned) {
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
        try {
            Set<CountersRange> set = sectorCounters.get(sectorName);
            Optional<CountersRange> maybeFreeCounterRange =
                    set.stream().filter(
                            range -> range.assignedInfo().isEmpty() && (range.range().to() - range.range().from() + 1) >= counterAssignment.counterCount()
                    ).findFirst();
            if (maybeFreeCounterRange.isPresent()) {
                Range range = assignInfoToAvailableCounterRange(counterAssignment, maybeFreeCounterRange.get(), set);

                // TODO: passengers?
                assignedFlights.addAll(counterAssignment.flights());

                return new Pair<>(range, 0);
            } else {
                int pending = addAssignmentToQueue(sectorName, counterAssignment);
                return new Pair<>(null, pending);
            }
        } finally {
            assignedFlightsLock.writeLock().unlock();
            sectorCountersLock.writeLock().unlock();
        }
    }

    private int addAssignmentToQueue(String sectorName, Assignment counterAssignment) {
        assignmentQueueLock.writeLock().lock();
        try {
            assigmentQueue.putIfAbsent(sectorName, new LinkedList<>());
            Queue<Assignment> queue = assigmentQueue.get(sectorName);
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
    public CountersRange freeCounters(String sectorName, int counterFrom, String airline) throws NoSuchElementException, HasPendingPassengersException {
        if(!hasSector(sectorName)) {
            throw new NoSuchElementException("Sector does not exist");
        }
        sectorCountersLock.writeLock().lock();
        try {
            Set<CountersRange> set = sectorCounters.get(sectorName);
            Optional<CountersRange> maybeToFreeCounterRange =
                    set.stream().filter(
                            range -> range.range().from() == counterFrom && range.assignedInfo().isPresent() && range.assignedInfo().get().airline().equals(airline)
                    ).findFirst();
            if(maybeToFreeCounterRange.isEmpty()) {
                throw new NoSuchElementException("Counter does not exist or not assigned");
            }

            // TODO: pending passengers

            CountersRange toFree = maybeToFreeCounterRange.get();

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
            if(maybeBefore.isPresent()) {
                set.remove(maybeBefore.get());
                newFrom = maybeBefore.get().range().from();
            }
            if(maybeAfter.isPresent()) {
                set.remove(maybeAfter.get());
                newTo = maybeAfter.get().range().to();
            }
            set.add(new CountersRange(new Range(newFrom, newTo)));

        } finally {
            sectorCountersLock.writeLock().unlock();
        }
        tryPendingAssignments(sectorName);
        return null;
    }

    @Override
    public Queue<Assignment> getQueuedAssignments(String sector) {
        assignedFlightsLock.readLock().lock();
        try {
            return assigmentQueue.getOrDefault(sector, new LinkedList<>());
        } finally {
            assignedFlightsLock.readLock().unlock();
        }
    }

    @Override
    public boolean hasPassengerInCounter(Range counterRange, String booking) {
        return false;
    }

    @Override
    public int addPassengerToQueue(Range counterRange, String booking) throws AlreadyExistsException {
        return 0;
    }

    @Override
    public List<Optional<String>> checkinCounters(String sector, int counterFrom, String airline) throws NoSuchElementException, UnauthorizedException {
        return List.of();
    }
}
