package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.exceptions.AlreadyExistsException;
import ar.edu.itba.pod.server.exceptions.UnauthorizedException;
import ar.edu.itba.pod.server.models.CountersRange;
import ar.edu.itba.pod.server.models.PendingAssignment;
import ar.edu.itba.pod.server.models.Range;
import ar.edu.itba.pod.server.models.Sector;
import ar.edu.itba.pod.server.utils.Pair;

import java.util.*;

public class CounterRepositorySynchronized implements CounterRepository{


    private final Map<String, Sector> sectors;

    private final Map<Range, Queue<String>> passengersInCounters;

    private int lastCounterId = 0;

    public CounterRepositorySynchronized() {
        this.sectors = new HashMap<>();
        this.passengersInCounters = new HashMap<>();
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
    public synchronized void assignCounterRange(String sector, CountersRange counterRange) {}

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
    public synchronized void addAssignmentToQueue(String sector, PendingAssignment assignment) {}

    @Override
    public synchronized void removeAssignmentFromQueue(
            String sector, PendingAssignment assignment) {}

    @Override
    public synchronized List<PendingAssignment> getQueuedAssignments(String sector) {
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
    public int addPassengerToQueue(Range counterRange, String booking) {
        return 0;
    }

    @Override
    public synchronized List<Optional<String>> checkinCounters(
            String sector, int counterFrom, String airline)
            throws NoSuchElementException, UnauthorizedException {
//
//        if (!hasSector(sector)) {
//            throw new NoSuchElementException("Sector does not exist");
//        }
//
//        List<CountersRange> counters = sectors.get(sector).countersRangeList();
//
//        Optional<CountersRange> countersStartingFrom =
//                counters.stream()
//                        .filter(range -> range.range().from() == counterFrom)
//                        .findFirst();
//
//        if (countersStartingFrom.isEmpty()) {
//            throw new NoSuchElementException("Counter does not exist");
//        }
//
//        if (countersStartingFrom.get().assignedInfo().isEmpty()
//                || !countersStartingFrom.get().assignedInfo().get().airline().equals(airline)) {
//            throw new UnauthorizedException("Unauthorized");
//        }
//
        return List.of();

    }
}
