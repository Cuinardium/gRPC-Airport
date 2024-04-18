package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.models.CountersRange;
import ar.edu.itba.pod.server.models.Range;

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

    @Override
    public boolean hasPassengerInCounter(Range counterRange, String booking) {
        return false;
    }

    @Override
    public void addSector(String sector) {
        
    }

    @Override
    public Range addCounters(String sector, int counterCount) {
        return null;
    }

    @Override
    public int addPassengerToQueue(Range counterRange, String booking) {
        return 0;
    }
}
