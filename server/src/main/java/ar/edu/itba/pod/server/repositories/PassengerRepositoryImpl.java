package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.exceptions.AlreadyExistsException;
import ar.edu.itba.pod.server.exceptions.FlightBelongsToOtherAirlineException;
import ar.edu.itba.pod.server.models.Passenger;

import java.util.List;
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
    public List<Passenger> getPassengers() {
        return List.copyOf(expectedPassengers.values());
    }

    @Override
    public void addPassenger(Passenger passenger) throws AlreadyExistsException,  FlightBelongsToOtherAirlineException {
        if (hasPassenger(passenger)) {
            throw new AlreadyExistsException("Passenger already exists");
        }

        if (!flightMatchesAirline(passenger.flight(), passenger.airline())) {
            throw new FlightBelongsToOtherAirlineException("Flight belongs to another airline");
        }

        expectedPassengers.put(passenger.booking(), passenger);
    }

    // Returns if all passengers that have the same flight as the given airline are from the same airline
    // If there is another airline that has passengers with the same flight, it returns false
    private boolean flightMatchesAirline(String flight, String airline) {
        return expectedPassengers.values().stream()
                .filter(passenger -> passenger.flight().equals(flight))
                .allMatch(passenger -> passenger.airline().equals(airline));
    }
}
