package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.exceptions.AlreadyExistsException;
import ar.edu.itba.pod.server.models.Checkin;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public interface CheckinRepository {

    void addCheckin(Checkin checkin) throws AlreadyExistsException;

    List<Checkin> getCheckins();
    List<Checkin> getCheckins(Predicate<Checkin> predicate);
    Optional<Checkin> getCheckin(String booking);

    boolean hasCheckins();
    boolean hasCheckin(String booking);
}
