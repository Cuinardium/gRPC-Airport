package ar.edu.itba.pod.server;

import ar.edu.itba.pod.server.events.EventManager;
import ar.edu.itba.pod.server.events.EventManagerImpl;
import ar.edu.itba.pod.server.repositories.*;
import ar.edu.itba.pod.server.services.*;

import io.grpc.BindableService;
import io.grpc.ServerBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Server {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    public static void main(String[] args) throws InterruptedException, IOException {
        logger.info(" Server Starting ...");

        CounterRepository counterRepository = new CounterRepositoryImpl();
        PassengerRepository passengerRepository = new PassengerRepositoryImpl();
        CheckinRepository checkinRepository = new CheckinRepositoryImpl();

        EventManager eventManager = new EventManagerImpl();

        BindableService adminService = new AdminService(counterRepository, passengerRepository);
        BindableService counterService =
                new CounterService(
                        counterRepository,
                        passengerRepository,
                        checkinRepository,
                        eventManager);
        BindableService passengerService =
                new PassengerService(
                        counterRepository,
                        passengerRepository,
                        checkinRepository,
                        eventManager);
        BindableService eventsService = new EventsService(passengerRepository, eventManager);
        BindableService queryService =
                new QueryService(counterRepository, checkinRepository);

        int port = 50051;
        io.grpc.Server server =
                ServerBuilder.forPort(port)
                        .addService(adminService)
                        .addService(counterService)
                        .addService(passengerService)
                        .addService(eventsService)
                        .addService(queryService)
                        .build();

        server.start();
        logger.info("Server started, listening on {}", port);

        server.awaitTermination();
        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread(
                                () -> {
                                    logger.info(
                                            "Shutting down gRPC server since JVM is shutting down");
                                    server.shutdown();
                                    logger.info("Server shut down");
                                }));
    }
}
