package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.models.Checkin;

import java.util.List;
import java.util.function.Predicate;

public interface CheckinRepository {

    void addCheckin(Checkin checkin);

    List<Checkin> getCheckins();

    List<Checkin> getCheckins(Predicate<Checkin> predicate);

    boolean hasCheckins();
}
