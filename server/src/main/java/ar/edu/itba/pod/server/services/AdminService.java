package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.grpc.admin.*;
import ar.edu.itba.pod.grpc.common.CounterRange;
import ar.edu.itba.pod.server.models.Passenger;
import ar.edu.itba.pod.server.models.Range;
import ar.edu.itba.pod.server.repositories.CounterRepository;
import ar.edu.itba.pod.server.repositories.PassengerRepository;

import com.google.protobuf.Empty;
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
    public void addSector(AddSectorRequest request, StreamObserver<com.google.protobuf.Empty> responseObserver) {
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

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void addCounters(AddCountersRequest request, StreamObserver<CounterRange> responseObserver) {
        String sector = request.getSectorName();

        if(sector.isEmpty()){
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("No sector was specified")
                            .asRuntimeException()
            );
            return;
        }

        if (!counterRepository.hasSector(sector)) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("The specificed sector does not exist")
                            .asRuntimeException()
            );
            return;
        }

        int counterCount = request.getCounterCount();

        if(counterCount <= 0){
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("The amount of counters needs to be a positive number")
                            .asRuntimeException()
            );
            return;
        }

        Range range = counterRepository.addCounters(sector, counterCount);

        responseObserver.onNext(
                CounterRange
                .newBuilder()
                .setFrom(range.from())
                .setTo(range.to())
                .build()
        );

        responseObserver.onCompleted();
    }

    @Override
    public void addPassenger(
            AddPassengerRequest request, StreamObserver<com.google.protobuf.Empty> responseObserver) {
        Passenger passenger = new Passenger(request.getBooking(), request.getFlight(), request.getAirline());

        if(passenger.booking().isEmpty() ||
                passenger.airline().isEmpty() ||
                passenger.flight().isEmpty()){

            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Booking, airline and flight must be provided.")
                            .asRuntimeException()
            );
            return;
        }

        if(passengerRepository.hasPassenger(passenger)){
            responseObserver.onError(
                    Status.ALREADY_EXISTS
                            .withDescription("This passenger was already added")
                            .asRuntimeException()
            );
            return;
        }

        passengerRepository.addPassenger(passenger);

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }


}
