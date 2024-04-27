package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.exceptions.*;
import ar.edu.itba.pod.server.models.*;
import ar.edu.itba.pod.server.utils.Pair;

import java.util.*;

public class CounterRepositorySynchronized implements CounterRepository {

    private final Map<String, Sector> sectors;

    private final Map<String, Queue<Assignment>> assignmentQueues;
    private final Set<String> assignedFlights;

    private final Map<Range, Queue<String>> passengersInCounters;

    private int lastCounterId = 0;

    public CounterRepositorySynchronized() {
        this.sectors = new HashMap<>();
        this.passengersInCounters = new HashMap<>();
        this.assignmentQueues = new HashMap<>();
        this.assignedFlights = new HashSet<>();
    }

    // -------- Sectors --------

    @Override
    public synchronized void addSector(String sector) throws AlreadyExistsException {
        if (hasSector(sector)) {
            throw new AlreadyExistsException("Sector already exists");
        }

        Sector newSector = new Sector(sector, List.of());

        sectors.put(sector, newSector);
    }

    @Override
    public synchronized boolean hasSector(String sector) {
        return sectors.containsKey(sector);
    }

    @Override
    public synchronized List<Sector> getSectors() {
        return List.copyOf(sectors.values());
    }

    @Override
    public synchronized Optional<Sector> getSector(String sectorName) {
        return Optional.ofNullable(sectors.get(sectorName));
    }

    // -------- Counters --------

    @Override
    public Range addCounters(String sector, int counterCount) throws NoSuchElementException {
        if (!hasSector(sector)) {
            throw new NoSuchElementException("Sector does not exist");
        }

        List<CountersRange> counters = sectors.get(sector).countersRangeList();

        if (counters.isEmpty()) {
            Range newRange = new Range(lastCounterId, lastCounterId + counterCount);
            counters.add(new CountersRange(newRange));

            lastCounterId += counterCount;
            return newRange;
        }

        // If the last counter is not the last one, we need to add a new range
        // If the last range is assigned, we need to add a new range
        // Otherwise, we just need to extend the last range
        CountersRange lastRange = counters.get(counters.size() - 1);
        if (lastRange.range().to() != lastCounterId || lastRange.assignedInfo().isPresent()) {
            Range newRange = new Range(lastCounterId, lastCounterId + counterCount);
            counters.add(new CountersRange(newRange));
            lastCounterId += counterCount;
            return newRange;
        }

        Range newRange = new Range(lastRange.range().from(), lastRange.range().to() + counterCount);
        lastCounterId += counterCount;

        counters.set(counters.size() - 1, new CountersRange(newRange));

        return newRange;
    }

    @Override
    public synchronized boolean hasCounters() {
        if (sectors.isEmpty()) {
            return false;
        }

        for (Sector sector : sectors.values()) {
            if (!sector.countersRangeList().isEmpty()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public synchronized List<CountersRange> getCounters() {
        return List.of();
    }

    @Override
    public synchronized List<CountersRange> getCountersFromSector(String sector) {
        return List.of();
    }

    @Override
    public synchronized Optional<CountersRange> getFlightCounters(String flight) {
        for (Sector sector : sectors.values()) {
            for (CountersRange range : sector.countersRangeList()) {
                if (range.assignedInfo().isPresent()
                        && range.assignedInfo().get().flights().contains(flight)) {
                    return Optional.of(range);
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public synchronized Optional<Pair<CountersRange, String>> getFlightCountersAndSector(
            String flight) {

        for (Sector sector : sectors.values()) {
            for (CountersRange range : sector.countersRangeList()) {
                if (range.assignedInfo().isPresent()
                        && range.assignedInfo().get().flights().contains(flight)) {
                    return Optional.of(new Pair<>(range, sector.sectorName()));
                }
            }
        }

        return Optional.empty();
    }

    // -------- Assignments --------

    @Override
    public Pair<Range, Integer> assignCounterAssignment(
            String sectorName, Assignment counterAssignment)
            throws FlightAlreadyAssignedException,
                    FlightAlreadyQueuedException,
                    FlightAlreadyCheckedInException {
        if (!hasSector(sectorName)) {
            throw new NoSuchElementException("Sector does not exist");
        }

        Sector sector = sectors.get(sectorName);

        // Check if there is a flight from the CounterAssignment
        // that is already assigned to an existing CountersRange
        boolean hasFlightAssigned =
                sector.countersRangeList().stream()
                        .filter(countersRange -> countersRange.assignedInfo().isPresent())
                        .anyMatch(
                                countersRange ->
                                        counterAssignment.flights().stream()
                                                .anyMatch(
                                                        countersRange.assignedInfo().get().flights()
                                                                ::contains));

        if (hasFlightAssigned) {
            throw new FlightAlreadyAssignedException("Flight already assigned to a counter");
        }

        // Check if there is a pending assignment that has at least one of the flights as the
        // current assignment
        boolean hasFlightQueued =
                assignmentQueues.get(sectorName).stream()
                        .anyMatch(
                                assignment ->
                                        counterAssignment.flights().stream()
                                                .anyMatch(assignment.flights()::contains));
        if (hasFlightQueued) {
            throw new FlightAlreadyQueuedException("Flight already queued");
        }

        // Check if any of the flights has been previously assigned to a counter
        // (which means that it has already been checked in)
        if (counterAssignment.flights().stream().anyMatch(assignedFlights::contains)) {
            throw new FlightAlreadyCheckedInException("Flight already checked in");
        }

        // TODO: Check if this takes order into consideration
        Optional<CountersRange> maybeAvailableCounterRange =
                sector.countersRangeList().stream()
                        .filter(
                                range ->
                                        range.assignedInfo().isEmpty()
                                                && (range.range().to() - range.range().from())
                                                        >= counterAssignment.counterCount())
                        .findFirst();

        // If there is an available range, assign the flights to it
        if (maybeAvailableCounterRange.isPresent()) {
            CountersRange availableCounterRange = maybeAvailableCounterRange.get();

            int from1 = availableCounterRange.range().from();
            int to1 = from1 + counterAssignment.counterCount() - 1;

            int from2 = to1 + 1;
            int to2 = availableCounterRange.range().to();

            CountersRange newAssignedCountersRange =
                    new CountersRange(
                            new Range(from1, to1),
                            new AssignedInfo(
                                    counterAssignment.airline(), counterAssignment.flights(), 0));


            for (int i = 0; i < sector.countersRangeList().size(); i++) {
                if (sector.countersRangeList().get(i).range().equals(availableCounterRange.range())) {
                    sector.countersRangeList().set(i, newAssignedCountersRange);

                    if (from2 <= to2) {
                        sector.countersRangeList().add(i + 1, new CountersRange(new Range(from2, to2)));
                    }
                }
            }

            return new Pair<>(newAssignedCountersRange.range(), 0);
        }

        int pendingAhead = addAssignmentToQueue(sectorName, counterAssignment);

        return new Pair<>(null, pendingAhead);
    }

    @Override
    public synchronized List<String> getPreviouslyAssignedFlights() {
        return List.of();
    }

    @Override
    public synchronized List<CountersRange> freeCounters(
            String sector, int counterFrom, String airline) throws NoSuchElementException {
        //        List<CountersRange> counters = getCountersFromSector(sector);
        //
        //        // TODO: check for optimization if list is ordered
        //        counters =
        //                counters.stream()
        //                        .filter(
        //                                range ->
        //                                        range.range().from() >= counterFrom
        //                                                && range.assignedInfo().isPresent()
        //                                                && range.assignedInfo()
        //                                                .get()
        //                                                .airline()
        //                                                .equals(airline))
        //                        .toList();
        //
        //        if (counters.isEmpty()) {
        //            throw new NoSuchElementException();
        //        }

        return List.of();
    }

    // -------- Queues-Assignments --------

    @Override
    public synchronized int addAssignmentToQueue(String sector, Assignment assignment) {
        return 0;
    }

    @Override
    public synchronized void removeAssignmentFromQueue(String sector, Assignment assignment) {}

    @Override
    public synchronized List<Assignment> getQueuedAssignments(String sector) {
        return List.of();
    }

    // --------- Queues-Passengers --------

    @Override
    public synchronized boolean hasPassengerInCounter(Range counterRange, String booking) {
        if (!passengersInCounters.containsKey(counterRange)) {
            return false;
        }

        return passengersInCounters.get(counterRange).contains(booking);
    }

    @Override
    public int addPassengerToQueue(Range counterRange, String booking)
            throws AlreadyExistsException {
        if (hasPassengerInCounter(counterRange, booking)) {
            throw new AlreadyExistsException("Passenger already in queue");
        }

        Queue<String> passengers = passengersInCounters.get(counterRange);

        passengers.add(booking);

        return passengers.size();
    }

    @Override
    public synchronized List<Optional<String>> checkinCounters(
            String sector, int counterFrom, String airline)
            throws NoSuchElementException, UnauthorizedException {

        if (!hasSector(sector)) {
            throw new NoSuchElementException("Sector does not exist");
        }

        List<CountersRange> counters = sectors.get(sector).countersRangeList();

        Optional<CountersRange> countersStartingFrom =
                counters.stream().filter(range -> range.range().from() == counterFrom).findFirst();

        if (countersStartingFrom.isEmpty() || countersStartingFrom.get().assignedInfo().isEmpty()) {
            throw new NoSuchElementException("Counter does not exist or not assigned");
        }

        if (!countersStartingFrom.get().assignedInfo().get().airline().equals(airline)) {
            throw new UnauthorizedException("Counter is not assigned to the airline");
        }

        CountersRange counter = countersStartingFrom.get();
        Queue<String> passengers = passengersInCounters.get(counter.range());

        List<Optional<String>> result = new ArrayList<>();

        for (int i = 0; i < counter.range().to() - counter.range().from(); i++) {
            if (passengers.isEmpty()) {
                result.add(Optional.empty());
            } else {
                result.add(Optional.of(passengers.poll()));
            }
        }

        return result;
    }
}
