package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.exceptions.AlreadyExistsException;
import ar.edu.itba.pod.server.models.Passenger;

import java.util.List;
import java.util.Optional;

public interface PassengerRepository {

    boolean hasAirline(String airline);
    boolean hasPassenger(Passenger passenger);

    Optional<Passenger> getPassenger(String booking);
    List<Passenger> getPassengers();

    void addPassenger(Passenger passenger) throws AlreadyExistsException;
}
