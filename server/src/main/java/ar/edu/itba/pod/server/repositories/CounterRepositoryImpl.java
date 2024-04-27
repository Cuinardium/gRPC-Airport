package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.exceptions.AlreadyExistsException;
import ar.edu.itba.pod.server.exceptions.UnauthorizedException;
import ar.edu.itba.pod.server.models.CountersRange;
import ar.edu.itba.pod.server.models.PendingAssignment;
import ar.edu.itba.pod.server.models.Range;
import ar.edu.itba.pod.server.models.Sector;
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
    public void assignCounterRange(String sector, CountersRange counterRange) {

    }

    @Override
    public void removeAssignmentFromQueue(String sector, PendingAssignment assignment) {

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
    public List<PendingAssignment> getQueuedAssignments(String sector) {
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
    public void addAssignmentToQueue(String sector, PendingAssignment assignment) {

    }

    @Override
    public int freeCounters(String sector, List<CountersRange> countersToFree) {
        return 0;
    }

    @Override
    public List<Optional<String>> checkinCounters(String sector, int counterFrom, String airline) throws NoSuchElementException, UnauthorizedException {
        return List.of();
    }
}
