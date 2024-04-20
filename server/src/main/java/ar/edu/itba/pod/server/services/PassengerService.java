package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.grpc.common.CounterRange;
import ar.edu.itba.pod.grpc.events.PassengerArrivedInfo;
import ar.edu.itba.pod.grpc.events.RegisterResponse;
import ar.edu.itba.pod.grpc.passenger.*;
import ar.edu.itba.pod.server.events.EventManager;
import ar.edu.itba.pod.server.models.Checkin;
import ar.edu.itba.pod.server.models.CountersRange;
import ar.edu.itba.pod.server.models.Passenger;
import ar.edu.itba.pod.server.models.Range;
import ar.edu.itba.pod.server.repositories.CheckinRepository;
import ar.edu.itba.pod.server.repositories.CounterRepository;
import ar.edu.itba.pod.server.repositories.PassengerRepository;

import ar.edu.itba.pod.server.utils.Pair;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class PassengerService extends PassengerServiceGrpc.PassengerServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(PassengerService.class);

    private final CounterRepository counterRepository;
    private final PassengerRepository passengerRepository;
    private final CheckinRepository checkinRepository;

    private final EventManager eventManager;

    public PassengerService(
            CounterRepository counterRepository,
            PassengerRepository passengerRepository,
            CheckinRepository checkinRepository,
            EventManager eventManager) {
        this.counterRepository = counterRepository;
        this.passengerRepository = passengerRepository;
        this.checkinRepository = checkinRepository;
        this.eventManager = eventManager;
    }

    @Override
    public void fetchCounter(
            FetchCounterRequest request, StreamObserver<FetchCounterResponse> responseObserver) {

        logger.debug("(passengerService/fetchCounter) Received fetch counter request");

        String booking = request.getBooking();

        if (booking.isEmpty()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("No booking was provided")
                            .asRuntimeException());

            logger.debug(
                    "(passengerService/fetchCounter) fetch counter request failed: booking field is empty");

            return;
        }

        logger.debug("(passengerService/fetchCounter) fetching passenger with booking {}", booking);
        Optional<Passenger> possiblePassenger = passengerRepository.getPassenger(booking);

        if (possiblePassenger.isEmpty()) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("No passenger expected with the given booking")
                            .asRuntimeException());

            logger.debug(
                    "(passengerService/fetchCounter) fetch counter request failed: no passenger found with booking {}",
                    booking);

            return;
        }

        Passenger passenger = possiblePassenger.get();

        FetchCounterResponse.Builder responseBuilder =
                FetchCounterResponse.newBuilder()
                        .setAirline(passenger.airline())
                        .setFlight(passenger.flight());

        logger.debug(
                "(passengerService/fetchCounter) fetching counters for flight {}",
                passenger.flight());
        Optional<CountersRange> possibleCounters =
                counterRepository.getFlightCounters(passenger.flight());

        if (possibleCounters.isPresent()) {

            logger.debug(
                    "(passengerService/fetchCounter) flight {} has assigned counters",
                    passenger.flight());

            Range counters = possibleCounters.get().range();
            int passengersInQueue =
                    possibleCounters
                            .get()
                            .assignedInfo()
                            .orElseThrow(IllegalStateException::new)
                            .passengersInQueue();

            responseBuilder
                    .setStatus(FlightStatus.FLIGHT_STATUS_ASSIGNED)
                    .setCounters(
                            CounterRange.newBuilder()
                                    .setFrom(counters.from())
                                    .setTo(counters.to())
                                    .build())
                    .setPassengersInQueue(passengersInQueue);
        } else {
            logger.debug(
                    "(passengerService/fetchCounter) flight {} has no assigned counters",
                    passenger.flight());

            responseBuilder.setStatus(FlightStatus.FLIGHT_STATUS_UNASSIGNED);
        }

        FetchCounterResponse response = responseBuilder.build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

        logger.debug(
                "(passengerService/fetchCounter) fetch counter request completed successfully");
    }

    @Override
    public void passengerCheckin(
            PassengerCheckinRequest request,
            StreamObserver<PassengerCheckinResponse> responseObserver) {

        logger.debug("(passengerService/passengerCheckin) Received passenger checkin request");

        String sector = request.getSectorName();
        String booking = request.getBooking();
        int firstCounter = request.getCounter();

        if (sector.isEmpty() || booking.isEmpty() || firstCounter <= 0) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Sector name, booking and counter must be provided")
                            .asRuntimeException());

            logger.debug(
                    "(passengerService/passengerCheckin) passenger checkin request failed: sector, booking or counter fields are empty");
            logger.debug(
                    "(passengerService/passengerCheckin) sector: {}, booking: {}, counter: {}",
                    sector,
                    booking,
                    firstCounter);

            return;
        }

        logger.debug(
                "(passengerService/passengerCheckin) fetching passenger with booking {}", booking);
        Optional<Passenger> possiblePassenger = passengerRepository.getPassenger(booking);

        if (possiblePassenger.isEmpty()) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("No passenger expected with the given booking")
                            .asRuntimeException());

            logger.debug(
                    "(passengerService/passengerCheckin) passenger checkin request failed: no passenger found with booking {}",
                    booking);

            return;
        }

        Passenger passenger = possiblePassenger.get();

        if (!counterRepository.hasSector(sector)) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("Provided sector does not exist")
                            .asRuntimeException());

            logger.debug(
                    "(passengerService/passengerCheckin) passenger checkin request failed: sector {} does not exist",
                    sector);

            return;
        }

        logger.debug(
                "(passengerService/passengerCheckin) fetching counters for flight {}",
                passenger.flight());
        Optional<CountersRange> possibleAssignedCounters =
                counterRepository.getFlightCounters(passenger.flight());

        if (possibleAssignedCounters.isEmpty()
                || possibleAssignedCounters.get().range().from() != firstCounter) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription(
                                    "The indicated does not exist or it is not accepting checkins for the booking flight")
                            .asRuntimeException());

            logger.debug(
                    "(passengerService/passengerCheckin) passenger checkin request failed: counter {} does not exist or is not accepting checkins for flight {}",
                    firstCounter,
                    passenger.flight());

            return;
        }

        Range assignedCounters = possibleAssignedCounters.get().range();

        if (counterRepository.hasPassengerInCounter(assignedCounters, booking)) {
            responseObserver.onError(
                    Status.ALREADY_EXISTS
                            .withDescription("Passenger is already waiting in counter queue")
                            .asRuntimeException());

            logger.debug(
                    "(passengerService/passengerCheckin) passenger checkin request failed: passenger is already waiting in counter queue");

            return;
        }

        if (checkinRepository.hasCheckin(booking)) {
            responseObserver.onError(
                    Status.ALREADY_EXISTS
                            .withDescription("Passenger has already completed checkin")
                            .asRuntimeException());

            logger.debug(
                    "(passengerService/passengerCheckin) passenger checkin request failed: passenger has already completed checkin");

            return;
        }

        int passengersInQueue = counterRepository.addPassengerToQueue(assignedCounters, booking);

        logger.debug(
                "(passengerService/passengerCheckin) passenger {} added to counter queue {}",
                booking,
                assignedCounters);

        RegisterResponse event =
                RegisterResponse.newBuilder()
                        .setPassengerArrivedInfo(
                                PassengerArrivedInfo.newBuilder()
                                        .setPassengersInQueue(passengersInQueue)
                                        .setBooking(booking)
                                        .setCounters(
                                                CounterRange.newBuilder()
                                                        .setFrom(assignedCounters.from())
                                                        .setTo(assignedCounters.to())
                                                        .build())
                                        .setFlight(passenger.flight())
                                        .setSectorName(sector))
                        .build();

        eventManager.notify(passenger.airline(), event);
        logger.debug(
                "(passengerService/passengerCheckin) passenger {} checkin event notified to airline {}",
                booking,
                passenger.airline());

        PassengerCheckinResponse response =
                PassengerCheckinResponse.newBuilder()
                        .setFlight(passenger.flight())
                        .setAirline(passenger.airline())
                        .setCounters(
                                CounterRange.newBuilder()
                                        .setFrom(assignedCounters.from())
                                        .setTo(assignedCounters.to())
                                        .build())
                        .setPassengersInQueue(passengersInQueue)
                        .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        logger.debug(
                "(passengerService/passengerCheckin) passenger checkin request completed successfully");
    }

    @Override
    public void passengerStatus(
            PassengerStatusRequest request,
            StreamObserver<PassengerStatusResponse> responseObserver) {

        logger.debug("(passengerService/passengerStatus) Received passenger status request");

        String booking = request.getBooking();

        if (booking.isEmpty()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("No booking was provided")
                            .asRuntimeException());

            logger.debug(
                    "(passengerService/passengerStatus) passenger status request failed: booking field is empty");

            return;
        }

        logger.debug(
                "(passengerService/passengerStatus) fetching passenger with booking {}", booking);
        Optional<Passenger> possiblePassenger = passengerRepository.getPassenger(booking);

        if (possiblePassenger.isEmpty()) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("No passenger expected with the given booking")
                            .asRuntimeException());

            logger.debug(
                    "(passengerService/passengerStatus) passenger status request failed: no passenger found with booking {}",
                    booking);

            return;
        }

        Passenger passenger = possiblePassenger.get();

        logger.debug(
                "(passengerService/passengerStatus) fetching counters for flight {}",
                passenger.flight());
        Optional<Pair<CountersRange, String>> possibleAssignedCounters =
                counterRepository.getFlightCountersAndSector(passenger.flight());

        if (possibleAssignedCounters.isEmpty()) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("No counters were assigned to the bookings flight")
                            .asRuntimeException());

            logger.debug(
                    "(passengerService/passengerStatus) passenger status request failed: no counters were assigned to the bookings flight");

            return;
        }

        Pair<CountersRange, String> counterSectorPair = possibleAssignedCounters.get();
        CountersRange assignedCounters = counterSectorPair.first();

        PassengerStatusResponse.Builder responseBuilder =
                PassengerStatusResponse.newBuilder()
                        .setAirline(passenger.airline())
                        .setFlight(passenger.flight())
                        .setSectorName(counterSectorPair.second());

        logger.debug("(passengerService/passengerStatus) fetching checkin for booking {}", booking);

        Optional<Checkin> possibleCheckin = checkinRepository.getCheckin(booking);

        if (possibleCheckin.isPresent()) {
            PassengerStatusResponse response =
                    responseBuilder
                            .setStatus(PassengerStatus.PASSENGER_STATUS_CHECKED_IN)
                            .setCheckedInCounter(possibleCheckin.get().counter())
                            .build();

            logger.debug(
                    "(passengerService/passengerStatus) passenger {} has already checked in",
                    booking);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            logger.debug(
                    "(passengerService/passengerStatus) passenger status request completed successfully");

            return;
        }

        Range range = assignedCounters.range();

        responseBuilder.setCounters(
                CounterRange.newBuilder().setFrom(range.from()).setTo(range.to()).build());

        if (counterRepository.hasPassengerInCounter(range, booking)) {
            int passengersInQueue =
                    assignedCounters
                            .assignedInfo()
                            .orElseThrow(IllegalStateException::new)
                            .passengersInQueue();

            responseBuilder
                    .setStatus(PassengerStatus.PASSENGER_STATUS_WAITING)
                    .setPassengersInQueue(passengersInQueue);

            logger.debug(
                    "(passengerService/passengerStatus) passenger {} is waiting in counter queue",
                    booking);

        } else {
            responseBuilder.setStatus(PassengerStatus.PASSENGER_STATUS_NOT_ARRIVED);

            logger.debug(
                    "(passengerService/passengerStatus) passenger {} has not arrived yet", booking);
        }

        PassengerStatusResponse response = responseBuilder.build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        logger.debug(
                "(passengerService/passengerStatus) passenger status request completed successfully");
    }
}
