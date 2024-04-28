package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.exceptions.AlreadyExistsException;
import ar.edu.itba.pod.server.exceptions.FlightBelongsToOtherAirlineException;
import ar.edu.itba.pod.server.models.Passenger;
import ar.edu.itba.pod.server.repositories.PassengerRepository;
import ar.edu.itba.pod.server.repositories.PassengerRepositoryImpl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class PassengerRepositoryTest {

    private final List<Passenger> passengers =
            List.of(
                    new Passenger("111111", "AR1234", "Aerolineas Argentinas"),
                    new Passenger("222222", "LA4321", "LATAM"),
                    new Passenger("333333", "AA1111", "American Airlines"),
                    new Passenger("444444", "BA2222", "British Airways"),
                    new Passenger("555555", "IB3333", "Iberia"),
                    new Passenger("666666", "AF4444", "Air France"));

    private PassengerRepository passengerRepository;

    @BeforeEach
    public final void setUp() {
        this.passengerRepository = new PassengerRepositoryImpl();
    }

    @Test
    public void addPassengers() throws AlreadyExistsException, FlightBelongsToOtherAirlineException {
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
    public void addPassengerAlreadyExists() throws AlreadyExistsException, FlightBelongsToOtherAirlineException {
        Passenger passenger = passengers.get(0);
        passengerRepository.addPassenger(passenger);

        Assertions.assertThrows(
                AlreadyExistsException.class, () -> passengerRepository.addPassenger(passenger));
    }

    @Test
    public void addPassengerFlightBelongsToOtherAirline() throws AlreadyExistsException, FlightBelongsToOtherAirlineException {
        Passenger passenger = passengers.get(0);
        passengerRepository.addPassenger(passenger);

        Passenger passenger2 = new Passenger("777777", "AR1234", "LATAM");
        Assertions.assertThrows(
                FlightBelongsToOtherAirlineException.class, () -> passengerRepository.addPassenger(passenger2));
    }

    @Test
    public void hasAirlineTrue() throws AlreadyExistsException, FlightBelongsToOtherAirlineException {
        for (Passenger passenger : passengers) {
            passengerRepository.addPassenger(passenger);
        }

        for (Passenger passenger : passengers) {
            Assertions.assertTrue(passengerRepository.hasAirline(passenger.airline()));
        }
    }

    @Test
    public void hasAirlineFalse() throws AlreadyExistsException, FlightBelongsToOtherAirlineException {
        for (Passenger passenger : passengers) {
            passengerRepository.addPassenger(passenger);
        }

        Assertions.assertFalse(passengerRepository.hasAirline("Lufthansa"));
    }

    @Test
    public void hasPassengerTrue() throws AlreadyExistsException, FlightBelongsToOtherAirlineException {
        for (Passenger passenger : passengers) {
            passengerRepository.addPassenger(passenger);
        }

        for (Passenger passenger : passengers) {
            Assertions.assertTrue(passengerRepository.hasPassenger(passenger));
        }
    }

    @Test
    public void hasPassengerFalse() throws AlreadyExistsException, FlightBelongsToOtherAirlineException {
        for (Passenger passenger : passengers) {
            passengerRepository.addPassenger(passenger);
        }

        Assertions.assertFalse(
                passengerRepository.hasPassenger(new Passenger("777777", "LH7777", "Lufthansa")));
    }

    @Test
    public void concurrentAddPassenger() throws AlreadyExistsException {
        CountDownLatch latch = new CountDownLatch(passengers.size());

        Map<String, Boolean> foundAirline = new ConcurrentHashMap<>();

        for (Passenger passenger : passengers) {
            new Thread(
                            () -> {
                                // Generate 1000 pasengers incrementing the booking number starting

                                // Randomly check if the airline exists
                                // Which iterates over all the passengers
                                int random = (int) (Math.random() * 1000);

                                for (int i = 0; i < 1000; i++) {
                                    try {
                                        passengerRepository.addPassenger(
                                                new Passenger(
                                                        String.valueOf(
                                                                Integer.parseInt(
                                                                                passenger.booking())
                                                                        + i),
                                                        passenger.flight(),
                                                        passenger.airline()));

                                        Thread.sleep((long) (Math.random() * 10));
                                    } catch (Exception e) {
                                    }

                                    if (i == random) {
                                        foundAirline.put(
                                                passenger.airline(),
                                                passengerRepository.hasAirline(
                                                        passenger.airline()));
                                    }
                                }

                                latch.countDown();
                            })
                    .start();
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
        }

        // Check all passengers were added
        for (Passenger passenger : passengers) {

            // Check that the 1000 passengers were added
            for (int i = 0; i < 1000; i++) {
                Optional<Passenger> possiblePassenger =
                        passengerRepository.getPassenger(
                                String.valueOf(Integer.parseInt(passenger.booking()) + i));

                Assertions.assertTrue(possiblePassenger.isPresent());
                Assertions.assertEquals(
                        new Passenger(
                                String.valueOf(Integer.parseInt(passenger.booking()) + i),
                                passenger.flight(),
                                passenger.airline()),
                        possiblePassenger.get());
            }
        }

        // Check that all airlines were found
        for (Passenger passenger : passengers) {
            Assertions.assertTrue(foundAirline.get(passenger.airline()));
        }
    }
}
