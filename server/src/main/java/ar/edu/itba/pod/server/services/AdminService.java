package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.grpc.admin.*;
import ar.edu.itba.pod.server.repositories.CounterRepository;
import ar.edu.itba.pod.server.repositories.PassengerRepository;

import io.grpc.stub.StreamObserver;

public class AdminService extends AdminServiceGrpc.AdminServiceImplBase {

    private final CounterRepository counterRepository;
    private final PassengerRepository passengerRepository;

    public AdminService(
            CounterRepository counterRepository, PassengerRepository passengerRepository) {
        this.counterRepository = counterRepository;
        this.passengerRepository = passengerRepository;
    }

    @Override
    public void addSector(
            AddSectorRequest request, StreamObserver<AddSectorResponse> responseObserver) {}

    @Override
    public void addCounters(
            AddCountersRequest request, StreamObserver<AddCountersResponse> responseObserver) {}

    @Override
    public void addPassenger(
            AddPassengerRequest request, StreamObserver<AddPassengerResponse> responseObserver) {}
}
