package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.exceptions.AlreadyExistsException;
import ar.edu.itba.pod.server.exceptions.NotFoundException;
import ar.edu.itba.pod.grpc.counter.CounterAssignment;
import ar.edu.itba.pod.server.models.CountersRange;
import ar.edu.itba.pod.server.models.Range;
import ar.edu.itba.pod.server.models.Sector;
import ar.edu.itba.pod.server.utils.Pair;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public interface CounterRepository {
    // Lists sorted by range
    List<CountersRange> getCounters();
    List<CountersRange> getCountersFromSector(String sector);
    Optional<CountersRange> getFlightCounters(String flight);
    Optional<Pair<CountersRange, String>> getFlightCountersAndSector(String flight);
    List<CounterAssignment> getQueuedAssignments(String sector);
    List<Sector> getSectors();
    Optional<Sector> getSector(String sectorName);
    List<String> getPreviouslyAssignedFlights();

    void assignCounterRange(String sector, CountersRange counterRange);

    void removeAssignmentFromQueue(String sector, CounterAssignment assignment);

    boolean hasCounters();
    boolean hasSector(String sector);
    boolean hasPassengerInCounter(Range counterRange, String booking);

    void addSector(String sector) throws AlreadyExistsException;
    Range addCounters(String sector, int counterCount) throws NotFoundException;
    int addPassengerToQueue(Range counterRange, String booking);
    void addAssignmentToQueue(String sector, CounterAssignment assignment);

    int freeCounters(String sector, List<CountersRange> countersToFree);
}
