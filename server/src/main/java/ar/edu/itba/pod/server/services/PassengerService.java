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

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.Optional;

public class PassengerService extends PassengerServiceGrpc.PassengerServiceImplBase {

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

        String booking = request.getBooking();

        if (booking.isEmpty()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("No booking was provided")
                            .asRuntimeException());

            return;
        }

        Optional<Passenger> possiblePassenger = passengerRepository.getPassenger(booking);

        if (possiblePassenger.isEmpty()) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("No passenger expected with the given booking")
                            .asRuntimeException());

            return;
        }

        Passenger passenger = possiblePassenger.get();

        FetchCounterResponse.Builder responseBuilder =
                FetchCounterResponse.newBuilder()
                        .setAirline(passenger.airline())
                        .setFlight(passenger.flight());

        Optional<CountersRange> possibleCounters =
                counterRepository.getFlightCounters(passenger.flight());

        if (possibleCounters.isPresent()) {
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
            responseBuilder.setStatus(FlightStatus.FLIGHT_STATUS_UNASSIGNED);
        }

        FetchCounterResponse response = responseBuilder.build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void passengerCheckin(
            PassengerCheckinRequest request,
            StreamObserver<PassengerCheckinResponse> responseObserver) {

        String sector = request.getSectorName();
        String booking = request.getBooking();
        int firstCounter = request.getCounter();

        if (sector.isEmpty() || booking.isEmpty() || firstCounter <= 0) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Sector name, booking and counter must be provided")
                            .asRuntimeException());

            return;
        }

        Optional<Passenger> possiblePassenger = passengerRepository.getPassenger(booking);

        if (possiblePassenger.isEmpty()) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("No passenger expected with the given booking")
                            .asRuntimeException());

            return;
        }

        Passenger passenger = possiblePassenger.get();

        if (!counterRepository.hasSector(sector)) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("Provided sector does not exist")
                            .asRuntimeException());

            return;
        }

        Optional<CountersRange> possibleAssignedCounters =
                counterRepository.getFlightCounters(passenger.flight());

        if (possibleAssignedCounters.isEmpty()
                || possibleAssignedCounters.get().range().from() != firstCounter) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription(
                                    "The indicated does not exist or it is not accepting checkins for the booking flight")
                            .asRuntimeException());
            return;
        }

        Range assignedCounters = possibleAssignedCounters.get().range();

        if (counterRepository.hasPassengerInCounter(assignedCounters, booking)) {
            responseObserver.onError(
                    Status.ALREADY_EXISTS
                            .withDescription("Passenger is already waiting in counter queue")
                            .asRuntimeException());

            return;
        }

        if (checkinRepository.hasCheckin(booking)) {
            responseObserver.onError(
                    Status.ALREADY_EXISTS
                            .withDescription("Passenger has already completed checkin")
                            .asRuntimeException());

            return;
        }

        int passengersInQueue = counterRepository.addPassengerToQueue(assignedCounters, booking);

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
    }

    @Override
    public void passengerStatus(
            PassengerStatusRequest request,
            StreamObserver<PassengerStatusResponse> responseObserver) {

        String booking = request.getBooking();

        if (booking.isEmpty()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("No booking was provided")
                            .asRuntimeException());

            return;
        }

        Optional<Passenger> possiblePassenger = passengerRepository.getPassenger(booking);

        if (possiblePassenger.isEmpty()) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("No passenger expected with the given booking")
                            .asRuntimeException());

            return;
        }

        Passenger passenger = possiblePassenger.get();

        Optional<CountersRange> possibleAssignedCounters =
                counterRepository.getFlightCounters(passenger.flight());

        if (possibleAssignedCounters.isEmpty()) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("No counters were assigned to the bookings flight")
                            .asRuntimeException());

            return;
        }

        CountersRange assignedCounters = possibleAssignedCounters.get();

        PassengerStatusResponse.Builder responseBuilder =
                PassengerStatusResponse.newBuilder()
                        .setAirline(passenger.airline())
                        .setFlight(passenger.flight())
                        .setSectorName(assignedCounters.sector());

        Optional<Checkin> possibleCheckin = checkinRepository.getCheckin(booking);

        if (possibleCheckin.isPresent()) {
            PassengerStatusResponse response =
                    responseBuilder
                            .setStatus(PassengerStatus.PASSENGER_STATUS_CHECKED_IN)
                            .setCheckedInCounter(possibleCheckin.get().counter())
                            .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

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
        } else {
            responseBuilder.setStatus(PassengerStatus.PASSENGER_STATUS_NOT_ARRIVED);
        }

        PassengerStatusResponse response = responseBuilder.build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
