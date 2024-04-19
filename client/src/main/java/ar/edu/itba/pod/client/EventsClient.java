package ar.edu.itba.pod.client;

import ar.edu.itba.pod.grpc.events.EventsServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class EventsClient {
    private static final Logger logger = LoggerFactory.getLogger(EventsClient.class);

    public static void main(String[] args) throws InterruptedException {
        logger.info("tpe1-g4 Events Client Starting ...");
        logger.info("grpc-com-patterns Events Client Starting ...");
        String svrAdd, host, port, action;

        try {
            svrAdd = Optional.ofNullable(System.getProperty("serverAddress")).orElseThrow(IllegalArgumentException::new);
        } catch (IllegalArgumentException e) {
            logger.error("You must specify a serverAddress");
            return;
        }

        try {
            String[] address = svrAdd.split(":");
            host = address[0];
            port = address[1];
        } catch (IndexOutOfBoundsException e) {
            logger.error("You must specify a port");
            return;
        }
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, Integer.parseInt(port))
                .usePlaintext()
                .build();

        EventsServiceGrpc.EventsServiceStub stub = EventsServiceGrpc.newStub(channel);

        try {
            action = Optional.ofNullable(System.getProperty("action")).orElseThrow(IllegalArgumentException::new);
        } catch (IllegalArgumentException e) {
            logger.error("You must specify an action");
            return;
        }
        try {
            executeAction(action, stub);
        } finally {
            channel.shutdown().awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private static void executeAction(String action, EventsServiceGrpc.EventsServiceStub stub) {
        switch (action) {
            case "fetchCounter":

                break;

            case "passengerCheckin":

                break;

            case "passengerStatus":

                break;
            default:
                // TODO: Exception?
                logger.error("Unknown action: {}", action);
                break;
        }
    }
}