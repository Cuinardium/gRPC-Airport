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


    // ----- Sectors -----
    void addSector(String sector) throws AlreadyExistsException;
    boolean hasSector(String sector);
    List<Sector> getSectors();
    Optional<Sector> getSector(String sectorName);

    // ----- Counters -----
    Range addCounters(String sector, int counterCount) throws NoSuchElementException;
    boolean hasCounters();
    Optional<CountersRange> getFlightCounters(String flight);
    Optional<Pair<CountersRange, String>> getFlightCountersAndSector(String flight);

    // ----- Assignments -----
    void assignCounterRange(String sector, CountersRange counterRange);
    List<String> getPreviouslyAssignedFlights();
    List<CountersRange> freeCounters(String sector, int counterFrom, String airline) throws NoSuchElementException, HasPendingPassengersException;

    // ----- Queues - Assignments -----
    void addAssignmentToQueue(String sector, PendingAssignment assignment);
    void removeAssignmentFromQueue(String sector, PendingAssignment assignment);
    List<PendingAssignment> getQueuedAssignments(String sector);

    // ----- Queues - Passengers -----
    boolean hasPassengerInCounter(Range counterRange, String booking);
    int addPassengerToQueue(Range counterRange, String booking) throws AlreadyExistsException;
    List<Optional<String>> checkinCounters(String sector, int counterFrom, String airline) throws NoSuchElementException, UnauthorizedException;
}
