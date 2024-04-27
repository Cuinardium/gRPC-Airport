package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.exceptions.*;
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
    Pair<Range, Integer> assignCounterAssignment(String sectorName, Assignment counterAssignment) throws FlightAlreadyAssignedException, FlightAlreadyQueuedException, FlightAlreadyCheckedInException;
    List<String> getPreviouslyAssignedFlights();
    List<CountersRange> freeCounters(String sector, int counterFrom, String airline) throws NoSuchElementException, HasPendingPassengersException;

    // ----- Queues - Assignments -----
    int addAssignmentToQueue(String sector, Assignment assignment);
    void removeAssignmentFromQueue(String sector, Assignment assignment);
    List<Assignment> getQueuedAssignments(String sector);

    // ----- Queues - Passengers -----
    boolean hasPassengerInCounter(Range counterRange, String booking);
    int addPassengerToQueue(Range counterRange, String booking) throws AlreadyExistsException;
    List<Optional<String>> checkinCounters(String sector, int counterFrom, String airline) throws NoSuchElementException, UnauthorizedException;
}
