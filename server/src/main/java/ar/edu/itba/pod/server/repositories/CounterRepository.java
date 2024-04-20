package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.models.CountersRange;
import ar.edu.itba.pod.server.models.Range;
import ar.edu.itba.pod.server.models.Sector;
import ar.edu.itba.pod.server.utils.Pair;

import java.util.List;
import java.util.Optional;

public interface CounterRepository {

    List<Sector> getSectors();
    Optional<Sector> getSector(String sectorName);

    // Lists sorted by range
    List<CountersRange> getCounters();
    List<CountersRange> getCountersFromSector(String sector);
    Optional<CountersRange> getFlightCounters(String flight);
    Optional<Pair<CountersRange, String>> getFlightCountersAndSector(String flight);

    boolean hasCounters();
    boolean hasSector(String sector);
    boolean hasPassengerInCounter(Range counterRange, String booking);

    void addSector(String sector);
    Range addCounters(String sector, int counterCount);
    int addPassengerToQueue(Range counterRange, String booking);
}
