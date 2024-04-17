package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.models.Checkin;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class CheckinRepositoryImpl implements CheckinRepository {
    @Override
    public void addCheckin(Checkin checkin) {}

    @Override
    public List<Checkin> getCheckins() {
        return List.of();
    }

    @Override
    public List<Checkin> getCheckins(Predicate<Checkin> predicate) {
        return List.of();
    }

    @Override
    public Optional<Checkin> getCheckin(String booking) {
        return Optional.empty();
    }

    @Override
    public boolean hasCheckins() {
        return false;
    }

    @Override
    public boolean hasCheckin(String booking) {
        return false;
    }
}
