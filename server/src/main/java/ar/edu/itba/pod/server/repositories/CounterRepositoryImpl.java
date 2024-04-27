package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.exceptions.*;
import ar.edu.itba.pod.server.models.*;
import ar.edu.itba.pod.server.utils.Pair;

import java.util.*;

public class CounterRepositoryImpl implements CounterRepository {

    @Override
    public void addSector(String sector) throws AlreadyExistsException {

    }

    @Override
    public boolean hasSector(String sector) {
        return false;
    }

    @Override
    public List<Sector> getSectors() {
        return List.of();
    }

    @Override
    public Optional<Sector> getSector(String sectorName) {
        return Optional.empty();
    }

    @Override
    public Range addCounters(String sector, int counterCount) throws NoSuchElementException {
        return null;
    }

    @Override
    public boolean hasCounters() {
        return false;
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
    public Pair<Range, Integer> assignCounterAssignment(String sectorName, Assignment counterAssignment) throws FlightAlreadyAssignedException, FlightAlreadyQueuedException, FlightAlreadyCheckedInException {
        return null;
    }

    @Override
    public List<String> getPreviouslyAssignedFlights() {
        return List.of();
    }

    @Override
    public CountersRange freeCounters(String sector, int counterFrom, String airline) throws NoSuchElementException, HasPendingPassengersException {
        return null;
    }

    @Override
    public Queue<Assignment> getQueuedAssignments(String sector) {
        return new LinkedList<>();
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
