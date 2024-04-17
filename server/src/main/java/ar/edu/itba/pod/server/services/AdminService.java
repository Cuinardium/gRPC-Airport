package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.grpc.admin.*;
import ar.edu.itba.pod.server.repositories.CounterRepository;
import ar.edu.itba.pod.server.repositories.PassengerRepository;

import io.grpc.Status;
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
    public void addSector(AddSectorRequest request, StreamObserver<AddSectorResponse> responseObserver) {
        String sector = request.getSectorName();

        if(sector.isEmpty()){
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("No sector was specified")
                            .asRuntimeException()
            );
            return;
        }

        if (counterRepository.hasSector(sector)) {
            responseObserver.onError(
                    Status.ALREADY_EXISTS
                            .withDescription("This sector was already added")
                            .asRuntimeException()
            );
            return;
        }

        counterRepository.addSector(sector);
        responseObserver.onCompleted();
    }

    @Override
    public void addCounters(AddCountersRequest request, StreamObserver<AddCountersResponse> responseObserver) {

    }

    @Override
    public void addPassenger(
            AddPassengerRequest request, StreamObserver<AddPassengerResponse> responseObserver) {}
}
