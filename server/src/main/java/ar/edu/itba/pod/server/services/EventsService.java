package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.grpc.events.EventsServiceGrpc;
import ar.edu.itba.pod.server.events.EventManager;

public class EventsService extends EventsServiceGrpc.EventsServiceImplBase {

    private final EventManager eventManager;

    public EventsService(EventManager eventManager) {
        this.eventManager = eventManager;
    }
}
