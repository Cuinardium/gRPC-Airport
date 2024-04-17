package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.models.Passenger;

import java.util.Optional;

public class PassengerRepositoryImpl implements PassengerRepository {
    @Override
    public boolean hasAirline(String airline) {
        return false;
    }

    @Override
    public Optional<Passenger> getPassenger(String booking) {
        return Optional.empty();
    }
}
