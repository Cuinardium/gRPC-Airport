package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.grpc.query.QueryServiceGrpc;
import ar.edu.itba.pod.server.queues.PassengerQueue;
import ar.edu.itba.pod.server.repositories.CheckinRepository;
import ar.edu.itba.pod.server.repositories.CounterRepository;

public class QueryService extends QueryServiceGrpc.QueryServiceImplBase {

    private final CounterRepository counterRepository;
    private final CheckinRepository checkinRepository;

    private final PassengerQueue passengerQueue;

    public QueryService(
            CounterRepository counterRepository,
            CheckinRepository checkinRepository,
            PassengerQueue passengerQueue) {
        this.counterRepository = counterRepository;
        this.checkinRepository = checkinRepository;
        this.passengerQueue = passengerQueue;
    }
}
