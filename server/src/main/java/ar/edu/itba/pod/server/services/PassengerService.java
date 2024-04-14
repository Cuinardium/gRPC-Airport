package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.grpc.passenger.*;
import ar.edu.itba.pod.server.events.EventManager;
import ar.edu.itba.pod.server.queues.PassengerQueue;
import ar.edu.itba.pod.server.repositories.CheckinRepository;
import ar.edu.itba.pod.server.repositories.CounterRepository;
import ar.edu.itba.pod.server.repositories.PassengerRepository;
import io.grpc.stub.StreamObserver;

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
            FetchCounterRequest request, StreamObserver<FetchCounterResponse> responseObserver) {}

    @Override
    public void passengerCheckin(
            PassengerCheckinRequest request,
            StreamObserver<PassengerCheckinResponse> responseObserver) {}

    @Override
    public void passengerStatus(
            PassengerStatusRequest request,
            StreamObserver<PassengerStatusResponse> responseObserver) {}
}
