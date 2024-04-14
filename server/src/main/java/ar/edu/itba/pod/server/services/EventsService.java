package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.grpc.events.*;
import ar.edu.itba.pod.server.events.EventManager;

import io.grpc.stub.StreamObserver;

public class EventsService extends EventsServiceGrpc.EventsServiceImplBase {

    private final EventManager eventManager;

    public EventsService(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    @Override
    public void unregister(
            UnregisterRequest request, StreamObserver<UnregisterResponse> responseObserver) {}

    @Override
    public void register(
            RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {}
}
