package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.models.Passenger;

import java.util.Optional;

public interface PassengerRepository {

    boolean hasAirline(String airline);

    Optional<Passenger> getPassenger(String booking);
}
