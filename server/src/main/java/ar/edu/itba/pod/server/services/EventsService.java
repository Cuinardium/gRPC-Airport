package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.grpc.events.*;
import ar.edu.itba.pod.server.events.EventManager;
import ar.edu.itba.pod.server.repositories.PassengerRepository;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class EventsService extends EventsServiceGrpc.EventsServiceImplBase {

    private final PassengerRepository passengerRepository;

    private final EventManager eventManager;

    public EventsService(PassengerRepository passengerRepository, EventManager eventManager) {
        this.passengerRepository = passengerRepository;
        this.eventManager = eventManager;
    }

    @Override
    public void unregister(
            UnregisterRequest request, StreamObserver<UnregisterResponse> responseObserver) {

        String airline = request.getAirline();

        if (airline.isEmpty()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("An airline has to be specified")
                            .asRuntimeException());

            return;
        }

        if (!eventManager.isRegistered(airline)) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("This airline is not registered for events")
                            .asRuntimeException());

            return;
        }

        eventManager.unregister(airline);

        responseObserver.onNext(UnregisterResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void register(
            RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {

        String airline = request.getAirline();

        if (airline.isEmpty()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("An airline has to be specified")
                            .asRuntimeException());

            return;
        }

        if (!passengerRepository.hasAirline(airline)) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription(
                                    "This airline has no flights nor passenger scheduled for this airport")
                            .asRuntimeException());

            return;
        }

        if (eventManager.isRegistered(airline)) {
            responseObserver.onError(
                    Status.ALREADY_EXISTS
                            .withDescription("This airline has already been registered for events")
                            .asRuntimeException());

            return;
        }

        eventManager.register(airline, responseObserver);
    }
}
