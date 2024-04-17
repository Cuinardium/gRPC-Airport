package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.models.CountersRange;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class CounterRepositoryImpl implements CounterRepository {
    @Override
    public List<CountersRange> getCounters() {
        return List.of();
    }

    @Override
    public List<CountersRange> getCounters(Predicate<CountersRange> predicate) {
        return List.of();
    }

    @Override
    public Optional<CountersRange> getFlightCounters(String flight) {
        return Optional.empty();
    }

    @Override
    public boolean hasCounters() {
        return false;
    }

    @Override
    public boolean hasSector(String sector) {
        return false;
    }
}
