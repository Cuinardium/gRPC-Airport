package ar.edu.itba.pod.repositories;

import ar.edu.itba.pod.server.exceptions.AlreadyExistsException;
import ar.edu.itba.pod.server.models.Passenger;
import ar.edu.itba.pod.server.repositories.PassengerRepository;
import ar.edu.itba.pod.server.repositories.PassengerRepositoryImpl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

public class PassengerRepositoryTest {

    private final List<Passenger> passengers =
            List.of(
                    new Passenger("123456", "AR1234", "Aerolineas Argentinas"),
                    new Passenger("654321", "LA4321", "LATAM"),
                    new Passenger("111111", "AA1111", "American Airlines"),
                    new Passenger("222222", "BA2222", "British Airways"),
                    new Passenger("333333", "IB3333", "Iberia"),
                    new Passenger("444444", "AF4444", "Air France"));

    private PassengerRepository passengerRepository;

    @BeforeEach
    public final void setUp() {
        this.passengerRepository = new PassengerRepositoryImpl();
    }

    @Test
    public void addPassengers() throws AlreadyExistsException {
        for (Passenger passenger : passengers) {
            passengerRepository.addPassenger(passenger);
        }

        for (Passenger passenger : passengers) {
            Optional<Passenger> possiblePassenger =
                    passengerRepository.getPassenger(passenger.booking());

            Assertions.assertTrue(possiblePassenger.isPresent());
            Assertions.assertEquals(passenger, possiblePassenger.get());
        }
    }

    @Test
    public void addPassengerAlreadyExists() throws AlreadyExistsException {
        Passenger passenger = passengers.get(0);
        passengerRepository.addPassenger(passenger);

        Assertions.assertThrows(
                AlreadyExistsException.class, () -> passengerRepository.addPassenger(passenger));
    }

    @Test
    public void hasAirlineTrue() throws AlreadyExistsException {
        for (Passenger passenger : passengers) {
            passengerRepository.addPassenger(passenger);
        }

        for (Passenger passenger : passengers) {
            Assertions.assertTrue(passengerRepository.hasAirline(passenger.airline()));
        }
    }

    @Test
    public void hasAirlineFalse() throws AlreadyExistsException {
        for (Passenger passenger : passengers) {
            passengerRepository.addPassenger(passenger);
        }

        Assertions.assertFalse(passengerRepository.hasAirline("Lufthansa"));
    }

    @Test
    public void hasPassengerTrue() throws AlreadyExistsException {
        for (Passenger passenger : passengers) {
            passengerRepository.addPassenger(passenger);
        }

        for (Passenger passenger : passengers) {
            Assertions.assertTrue(passengerRepository.hasPassenger(passenger));
        }
    }

    @Test
    public void hasPassengerFalse() throws AlreadyExistsException {
        for (Passenger passenger : passengers) {
            passengerRepository.addPassenger(passenger);
        }

        Assertions.assertFalse(
                passengerRepository.hasPassenger(new Passenger("777777", "LH7777", "Lufthansa")));
    }
}
