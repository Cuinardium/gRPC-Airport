package ar.edu.itba.pod.client;

import ar.edu.itba.pod.grpc.passenger.PassengerServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class PassengerClient {
    private static final Logger logger = LoggerFactory.getLogger(PassengerClient.class);

    public static void main(String[] args) throws InterruptedException {
        logger.info("tpe1-g4 Passenger Client Starting ...");
        logger.info("grpc-com-patterns Passenger Client Starting ...");
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

        PassengerServiceGrpc.PassengerServiceBlockingStub stub = PassengerServiceGrpc.newBlockingStub(channel);

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

    private static void executeAction(String action, PassengerServiceGrpc.PassengerServiceBlockingStub stub) {
        switch (action) {
            case "register":

                break;

            case "unregister":

                break;

            case "history":

                break;
            default:
                // TODO: Exception?
                logger.error("Unknown action: {}", action);
                break;
        }
    }
}
