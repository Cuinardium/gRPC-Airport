package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.grpc.admin.Passenger;

import java.util.Optional;

public interface PassengerRepository {

    boolean hasAirline(String airline);
    boolean hasPassenger(Passenger passenger);

    Optional<Passenger> getPassenger(String booking);

    void addPassenger(Passenger passenger);
}
