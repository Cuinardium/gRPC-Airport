package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.grpc.passenger.PassengerServiceGrpc;
import ar.edu.itba.pod.server.events.EventManager;
import ar.edu.itba.pod.server.queues.PassengerQueue;
import ar.edu.itba.pod.server.repositories.CheckinRepository;
import ar.edu.itba.pod.server.repositories.CounterRepository;
import ar.edu.itba.pod.server.repositories.PassengerRepository;

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
}
