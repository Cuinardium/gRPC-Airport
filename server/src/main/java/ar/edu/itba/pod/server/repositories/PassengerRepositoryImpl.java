package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.models.Passenger;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PassengerRepositoryImpl implements PassengerRepository {

    private final Collection<Passenger> expectedPassengers;

    public PassengerRepositoryImpl() {
        this.expectedPassengers = new ConcurrentLinkedQueue<>();
    }

    @Override
    public boolean hasAirline(String airline) {
        return expectedPassengers.stream()
                .anyMatch(passenger -> passenger.airline().equals(airline));
    }

    @Override
    public boolean hasPassenger(Passenger passenger) {
        return expectedPassengers.contains(passenger);
    }

    @Override
    public Optional<Passenger> getPassenger(String booking) {
        return expectedPassengers.stream()
                .filter(passenger -> passenger.booking().equals(booking))
                .findFirst();
    }

    @Override
    public void addPassenger(Passenger passenger) {
        expectedPassengers.add(passenger);
    public void addPassenger(Passenger passenger) throws AlreadyExistsException {
        if (hasPassenger(passenger)) {
            throw new AlreadyExistsException("Passenger already exists");
        }

    }
}
