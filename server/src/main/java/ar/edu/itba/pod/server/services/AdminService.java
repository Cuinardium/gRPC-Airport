package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.grpc.admin.AdminServiceGrpc;
import ar.edu.itba.pod.server.repositories.CounterRepository;
import ar.edu.itba.pod.server.repositories.PassengerRepository;

public class AdminService extends AdminServiceGrpc.AdminServiceImplBase {

    private final CounterRepository counterRepository;
    private final PassengerRepository passengerRepository;

    public AdminService(
            CounterRepository counterRepository, PassengerRepository passengerRepository) {
        this.counterRepository = counterRepository;
        this.passengerRepository = passengerRepository;
    }
}
