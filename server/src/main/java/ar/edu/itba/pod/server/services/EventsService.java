package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.grpc.events.*;
import ar.edu.itba.pod.server.events.EventManager;
import ar.edu.itba.pod.server.repositories.PassengerRepository;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventsService extends EventsServiceGrpc.EventsServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(EventsService.class);

    private final PassengerRepository passengerRepository;

    private final EventManager eventManager;

    public EventsService(PassengerRepository passengerRepository, EventManager eventManager) {
        this.passengerRepository = passengerRepository;
        this.eventManager = eventManager;
    }

    @Override
    public void unregister(
            UnregisterRequest request, StreamObserver<UnregisterResponse> responseObserver) {

        logger.debug("(eventsService/unregister) received request");

        String airline = request.getAirline();

        if (airline.isEmpty()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("An airline has to be specified")
                            .asRuntimeException());

            logger.debug("(eventsService/unregister) request failed: an airline has to be specified");

            return;
        }

        logger.debug("(eventsService/unregister) checking if airline {} is registered", airline);

        if (!eventManager.isRegistered(airline)) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("This airline is not registered for events")
                            .asRuntimeException());

            logger.debug("(eventsService/unregister) request failed: airline {} is not registered", airline);

            return;
        }

        logger.debug("(eventsService/unregister) unregistering airline {}", airline);
        eventManager.unregister(airline);

        responseObserver.onNext(UnregisterResponse.newBuilder().build());
        responseObserver.onCompleted();

        logger.debug("(eventsService/unregister) request completed successfully");
    }

    @Override
    public void register(
            RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {

        logger.debug("(eventsService/register) received request");

        String airline = request.getAirline();

        if (airline.isEmpty()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("An airline has to be specified")
                            .asRuntimeException());

            logger.debug("(eventsService/register) request failed: an airline has to be specified");

            return;
        }

        logger.debug("(eventsService/register) checking if airline {} has expected passengers", airline);

        if (!passengerRepository.hasAirline(airline)) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription(
                                    "This airline has no flights nor passenger scheduled for this airport")
                            .asRuntimeException());

            logger.debug(
                    "(eventsService/register) request failed: airline {} has no flights nor passenger scheduled",
                    airline);

            return;
        }

        logger.debug("(eventsService/register) checking if airline {} is already registered", airline);

        if (eventManager.isRegistered(airline)) {
            responseObserver.onError(
                    Status.ALREADY_EXISTS
                            .withDescription("This airline has already been registered for events")
                            .asRuntimeException());

            logger.debug("(eventsService/register) request failed: airline {} is already registered", airline);

            return;
        }

        logger.debug("(eventsService/register) registering airline {}", airline);
        eventManager.register(airline, responseObserver);

        logger.debug("(eventsService/register) request completed successfully");
    }
}
