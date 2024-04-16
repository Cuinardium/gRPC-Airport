package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.models.CountersRange;
import ar.edu.itba.pod.server.models.Range;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public interface CounterRepository {

    // Lists sorted by range
    List<CountersRange> getCounters();
    List<CountersRange> getCounters(Predicate<CountersRange> predicate);

    Optional<Range> getFlightCounters(String flight);

    boolean hasCounters();
    boolean hasSector(String sector);
}
