package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.grpc.query.*;
import ar.edu.itba.pod.server.queues.PassengerQueue;
import ar.edu.itba.pod.server.repositories.CheckinRepository;
import ar.edu.itba.pod.server.repositories.CounterRepository;

import io.grpc.stub.StreamObserver;

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

    @Override
    public void checkins(CheckinInfo request, StreamObserver<CheckinsResponse> responseObserver) {}

    @Override
    public void counters(
            CountersRequest request, StreamObserver<CountersResponse> responseObserver) {}
}
