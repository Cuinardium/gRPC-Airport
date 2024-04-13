package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.grpc.counter.CounterServiceGrpc;
import ar.edu.itba.pod.server.events.EventManager;
import ar.edu.itba.pod.server.queues.AirlineQueue;
import ar.edu.itba.pod.server.queues.PassengerQueue;
import ar.edu.itba.pod.server.repositories.CheckinRepository;
import ar.edu.itba.pod.server.repositories.CounterRepository;
import ar.edu.itba.pod.server.repositories.PassengerRepository;

public class CounterService extends CounterServiceGrpc.CounterServiceImplBase {

    private final CounterRepository counterRepository;
    private final PassengerRepository passengerRepository;
    private final CheckinRepository checkinRepository;

    private final PassengerQueue passengerQueue;
    private final AirlineQueue airlineQueue;

    private final EventManager eventManager;

    public CounterService(
            CounterRepository counterRepository,
            PassengerRepository passengerRepository,
            CheckinRepository checkinRepository,
            PassengerQueue passengerQueue,
            AirlineQueue airlineQueue,
            EventManager eventManager) {
        this.counterRepository = counterRepository;
        this.passengerRepository = passengerRepository;
        this.checkinRepository = checkinRepository;
        this.passengerQueue = passengerQueue;
        this.airlineQueue = airlineQueue;
        this.eventManager = eventManager;
    }
}
