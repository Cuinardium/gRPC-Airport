package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.grpc.common.CounterRange;
import ar.edu.itba.pod.grpc.events.PassengerArrivedInfo;
import ar.edu.itba.pod.grpc.events.RegisterResponse;
import ar.edu.itba.pod.grpc.passenger.*;
import ar.edu.itba.pod.server.events.EventManager;
import ar.edu.itba.pod.server.models.Passenger;
import ar.edu.itba.pod.server.models.Range;
import ar.edu.itba.pod.server.queues.PassengerQueue;
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

    private final PassengerQueue passengerQueue;

    private final EventManager eventManager;

    public PassengerService(
            CounterRepository counterRepository,
            PassengerRepository passengerRepository,
            CheckinRepository checkinRepository,
            PassengerQueue passengerQueue,
            EventManager eventManager) {
        this.counterRepository = counterRepository;
        this.passengerRepository = passengerRepository;
        this.checkinRepository = checkinRepository;
        this.passengerQueue = passengerQueue;
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

        Optional<Range> possibleCounters = counterRepository.getFlightCounters(passenger.flight());

        if (possibleCounters.isPresent()) {
            Range counters = possibleCounters.get();
            Integer passengersInQueue =
                    passengerQueue
                            .getPassengersInCounter(counters)
                            .orElseThrow(IllegalStateException::new);

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

        Optional<Range> possibleAssignedCounters =
                counterRepository.getFlightCounters(passenger.flight());

        if (possibleAssignedCounters.isEmpty()
                || possibleAssignedCounters.get().from() != firstCounter) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription(
                                    "The indicated does not exist or it is not accepting checkins for the booking flight")
                            .asRuntimeException());
            return;
        }

        Range assignedCounters = possibleAssignedCounters.get();

        if (passengerQueue.hasPassengerInCounter(assignedCounters, booking)) {
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

        passengerQueue.addPassengerToQueue(assignedCounters, booking);
        int passengersInQueue =
                passengerQueue
                        .getPassengersInCounter(assignedCounters)
                        .orElseThrow(IllegalStateException::new);

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
            StreamObserver<PassengerStatusResponse> responseObserver) {}
}
