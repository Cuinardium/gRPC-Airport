package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.models.CountersRange;

import java.util.List;
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

}
