package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.models.Checkin;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

public class CheckinRepositoryImpl implements CheckinRepository {

    private final Collection<Checkin> checkins;

    public CheckinRepositoryImpl() {
        this.checkins = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void addCheckin(Checkin checkin) {
        checkins.add(checkin);
    }

    @Override
    public List<Checkin> getCheckins() {
        return getCheckins((checkin -> true));
    }

    @Override
    public List<Checkin> getCheckins(Predicate<Checkin> predicate) {
        Checkin[] checkinsArray = checkins.toArray(new Checkin[0]);
        return Arrays.stream(checkinsArray).filter(predicate).toList();
    }

    @Override
    public Optional<Checkin> getCheckin(String booking) {
        return checkins.stream().filter(checkin -> checkin.booking().equals(booking)).findFirst();
    }

    @Override
    public boolean hasCheckins() {
        return !checkins.isEmpty();
    }

    @Override
    public boolean hasCheckin(String booking) {
        return getCheckin(booking).isPresent();
    }
}
