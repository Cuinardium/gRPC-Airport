package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.exceptions.AlreadyExistsException;
import ar.edu.itba.pod.server.exceptions.NotFoundException;
import ar.edu.itba.pod.server.models.CountersRange;
import ar.edu.itba.pod.server.models.Range;
import ar.edu.itba.pod.server.models.Sector;
import ar.edu.itba.pod.server.utils.Pair;

import java.util.List;
import java.util.Optional;

public class CounterRepositoryImpl implements CounterRepository {
    @Override
    public List<Sector> getSectors() {
        return List.of();
    }

    @Override
    public Optional<Sector> getSector(String sectorName) {
        return Optional.empty();
    }

    @Override
    public List<CountersRange> getCounters() {
        return List.of();
    }

    @Override
    public List<CountersRange> getCountersFromSector(String sector) {
        return List.of();
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
    public void addSector(String sector) throws AlreadyExistsException {}

    @Override
    public Range addCounters(String sector, int counterCount) throws NotFoundException {
        return null;
    }

    @Override
    public int addPassengerToQueue(Range counterRange, String booking) {
        return 0;
    }
}
