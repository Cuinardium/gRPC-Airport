package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.grpc.admin.*;
import ar.edu.itba.pod.grpc.common.CounterRange;
import ar.edu.itba.pod.server.exceptions.AlreadyExistsException;
import ar.edu.itba.pod.server.exceptions.FlightBelongsToOtherAirlineException;
import ar.edu.itba.pod.server.models.Passenger;
import ar.edu.itba.pod.server.models.Range;
import ar.edu.itba.pod.server.repositories.CounterRepository;
import ar.edu.itba.pod.server.repositories.PassengerRepository;

import com.google.protobuf.Empty;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NoSuchElementException;

public class AdminService extends AdminServiceGrpc.AdminServiceImplBase {

    private final Logger logger = LoggerFactory.getLogger(AdminService.class);

    private final CounterRepository counterRepository;
    private final PassengerRepository passengerRepository;

    public AdminService(
            CounterRepository counterRepository, PassengerRepository passengerRepository) {
        this.counterRepository = counterRepository;
        this.passengerRepository = passengerRepository;
    }

    @Override
    public void addSector(AddSectorRequest request, StreamObserver<Empty> responseObserver) {
        String sector = request.getSectorName();

        logger.debug("(adminService/addSector) received request to add sector");

        if (sector.isEmpty()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("No sector was specified")
                            .asRuntimeException());

            logger.debug("(adminService/addSector) request failed: no sector was specified");

            return;
        }

        try {
            counterRepository.addSector(sector);

            logger.debug("(adminService/addSector) sector added: {}", sector);
        } catch (AlreadyExistsException e) {
            responseObserver.onError(
                    Status.ALREADY_EXISTS
                            .withDescription("This sector was already added")
                            .asRuntimeException());

            logger.debug(
                    "(adminService/addSector) request failed: sector {} already exists", sector);
            return;
        }

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void addCounters(
            AddCountersRequest request, StreamObserver<CounterRange> responseObserver) {
        String sector = request.getSectorName();

        logger.debug("(adminService/addCounters) received request to add counters");

        if (sector.isEmpty()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("No sector was specified")
                            .asRuntimeException());

            logger.debug("(adminService/addCounters) request failed: no sector was specified");
            return;
        }

        int counterCount = request.getCounterCount();
        if (counterCount <= 0) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("The amount of counters needs to be a positive number")
                            .asRuntimeException());

            logger.debug(
                    "(adminService/addCounters) request failed: counter count must be a positive number (received {})",
                    counterCount);

            return;
        }

        Range range;
        try {
            range = counterRepository.addCounters(sector, counterCount);

            logger.debug(
                    "(adminService/addCounters) counters added to sector {}: from {} to {}",
                    sector,
                    range.from(),
                    range.to());

        } catch (NoSuchElementException e) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("The specified sector does not exist")
                            .asRuntimeException());
            return;
        }

        responseObserver.onNext(
                CounterRange.newBuilder().setFrom(range.from()).setTo(range.to()).build());

        responseObserver.onCompleted();
    }

    @Override
    public void addPassenger(AddPassengerRequest request, StreamObserver<Empty> responseObserver) {

        logger.debug("(adminService/addPassenger) received request to add passenger");

        Passenger passenger =
                new Passenger(request.getBooking(), request.getFlight(), request.getAirline());

        if (passenger.booking().isEmpty()
                || passenger.airline().isEmpty()
                || passenger.flight().isEmpty()) {

            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Booking, airline and flight must be provided.")
                            .asRuntimeException());

            logger.debug(
                    "(adminService/addPassenger) request failed: booking, airline and flight must be provided, received booking: {}, airline: {}, flight: {}",
                    passenger.booking(),
                    passenger.airline(),
                    passenger.flight());

            return;
        }

        try {
            passengerRepository.addPassenger(passenger);

            logger.debug("(adminService/addPassenger) passenger added: {}", passenger);
        } catch (AlreadyExistsException e) {
            responseObserver.onError(
                    Status.ALREADY_EXISTS
                            .withDescription("This passenger was already added")
                            .asRuntimeException());

            logger.debug(
                    "(adminService/addPassenger) request failed: passenger {} already exists",
                    passenger);

            return;
        } catch (FlightBelongsToOtherAirlineException e) {
            responseObserver.onError(
                    Status.PERMISSION_DENIED
                            .withDescription("Flight belongs to another airline")
                            .asRuntimeException());

            logger.debug(
                    "(adminService/addPassenger) request failed: flight {} belongs to another airline",
                    passenger.flight());

            return;
        }

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
