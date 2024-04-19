package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.exceptions.AlreadyExistsException;
import ar.edu.itba.pod.server.models.Passenger;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class PassengerRepositoryImpl implements PassengerRepository {

    private final Map<String, Passenger> expectedPassengers;

    public PassengerRepositoryImpl() {
        this.expectedPassengers = new ConcurrentHashMap<>();
    }

    @Override
    public boolean hasAirline(String airline) {
        return expectedPassengers.values().stream()
                .anyMatch(passenger -> passenger.airline().equals(airline));
    }

    @Override
    public boolean hasPassenger(Passenger passenger) {
        return getPassenger(passenger.booking()).isPresent();
    }

    @Override
    public Optional<Passenger> getPassenger(String booking) {
        return Optional.ofNullable(expectedPassengers.get(booking));
    }

    @Override
    public void addPassenger(Passenger passenger) throws AlreadyExistsException {
        if (hasPassenger(passenger)) {
            throw new AlreadyExistsException("Passenger already exists");
        }

        expectedPassengers.put(passenger.booking(), passenger);
    }
}
