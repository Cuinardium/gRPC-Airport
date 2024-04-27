package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.exceptions.AlreadyExistsException;
import ar.edu.itba.pod.server.exceptions.HasPendingPassengersException;
import ar.edu.itba.pod.server.exceptions.UnauthorizedException;
import ar.edu.itba.pod.server.models.*;
import ar.edu.itba.pod.server.utils.Pair;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public interface CounterRepository {
    // Lists sorted by range
    List<CountersRange> getCounters();
    List<CountersRange> getCountersFromSector(String sector);
    Optional<CountersRange> getFlightCounters(String flight);
    Optional<Pair<CountersRange, String>> getFlightCountersAndSector(String flight);
    List<PendingAssignment> getQueuedAssignments(String sector);
    List<Sector> getSectors();
    Optional<Sector> getSector(String sectorName);
    List<String> getPreviouslyAssignedFlights();

    void assignCounterRange(String sector, CountersRange counterRange);

    void removeAssignmentFromQueue(String sector, PendingAssignment assignment);

    boolean hasCounters();
    boolean hasSector(String sector);
    boolean hasPassengerInCounter(Range counterRange, String booking);

    void addSector(String sector) throws AlreadyExistsException;
    Range addCounters(String sector, int counterCount) throws NoSuchElementException;
    int addPassengerToQueue(Range counterRange, String booking) throws AlreadyExistsException;
    void addAssignmentToQueue(String sector, PendingAssignment assignment);

    List<CountersRange> freeCounters(String sector, int counterFrom, String airline) throws NoSuchElementException, HasPendingPassengersException;

    List<Optional<String>> checkinCounters(String sector, int counterFrom, String airline) throws NoSuchElementException, UnauthorizedException;
}
