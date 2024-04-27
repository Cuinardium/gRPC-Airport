package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.grpc.counter.CounterAssignment;
import ar.edu.itba.pod.server.exceptions.*;
import ar.edu.itba.pod.server.models.*;
import ar.edu.itba.pod.server.utils.Pair;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class CounterRepositoryImpl implements CounterRepository {
    @Override
    public List<Sector> getSectors() {
        return List.of();
    }

    @Override
    public Optional<Sector> getSector(String sectorName) {
        return Optional.empty();
    }

    @Override
    public List<String> getPreviouslyAssignedFlights() {
        return List.of();
    }

    @Override
    public void removeAssignmentFromQueue(String sector, Assignment assignment) {

    }

    @Override
    public List<CountersRange> getCounters() {
        return List.of();
    }

    @Override
    public List<CountersRange> getCountersFromSector(String sector) {
        return List.of();
    }

    @Override
    public Optional<CountersRange> getFlightCounters(String flight) {
        return Optional.empty();
    }

    @Override
    public Optional<Pair<CountersRange, String>> getFlightCountersAndSector(String flight) {
        return Optional.empty();
    }

    @Override
    public Pair<Range, Integer> assignCounterAssignment(String sectorName, CounterAssignment counterAssignment) throws FlightAlreadyAssignedException, FlightAlreadyQueuedException, FlightAlreadyCheckedInException {
        return null;
    }

    @Override
    public List<Assignment> getQueuedAssignments(String sector) {
        return List.of();
    }

    @Override
    public boolean hasCounters() {
        return false;
    }

    @Override
    public boolean hasSector(String sector) {
        return false;
    }

    @Override
    public boolean hasPassengerInCounter(Range counterRange, String booking) {
        return false;
    }

    @Override
    public void addSector(String sector) throws AlreadyExistsException {}

    @Override
    public Range addCounters(String sector, int counterCount) throws NoSuchElementException {
        return null;
    }

    @Override
    public int addPassengerToQueue(Range counterRange, String booking) {
        return 0;
    }

    @Override
    public int addAssignmentToQueue(String sector, Assignment assignment) {

        return 0;
    }

    @Override
    public List<CountersRange> freeCounters(String sector, int counterFrom, String airline) throws NoSuchElementException {
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

    @Override
    public List<Optional<String>> checkinCounters(String sector, int counterFrom, String airline) throws NoSuchElementException, UnauthorizedException {
        return List.of();
    }
}
