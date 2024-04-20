package ar.edu.itba.pod.client;

import ar.edu.itba.pod.grpc.passenger.*;
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
        String booking= Optional.ofNullable(System.getProperty("booking")).orElseThrow(IllegalArgumentException::new);
        switch (action) {
            case "fetchCounter":
                FetchCounterRequest fetchCounterRequest = FetchCounterRequest
                        .newBuilder()
                        .setBooking(booking)
                        .build();
                FetchCounterResponse fetchCounterResponse= stub.fetchCounter(fetchCounterRequest);
                if (fetchCounterResponse.getStatus() == FlightStatus.FLIGHT_STATUS_ASSIGNED){
                    System.out.println("Flight " + fetchCounterResponse.getFlight() + " from " + fetchCounterResponse.getAirline() + " is now checking in at counters ("
                            + fetchCounterResponse.getCounters().getFrom() + "- " + fetchCounterResponse.getCounters().getTo() + ") in Sector " + fetchCounterResponse.getSector()
                            + " with " + fetchCounterResponse.getPassengersInQueue() + " people in line");
                }else {
                    System.out.println("Flight " + fetchCounterResponse.getFlight() + " from " + fetchCounterResponse.getAirline() + " has no counters assigned yet");
                }
                break;

            case "passengerCheckin":
                String sector = Optional.ofNullable(System.getProperty("sector")).orElseThrow(IllegalArgumentException::new);
                int counter = Integer.parseInt(Optional.ofNullable(System.getProperty("counter")).orElseThrow(IllegalArgumentException::new));
                PassengerCheckinRequest passengerCheckinRequest= PassengerCheckinRequest
                        .newBuilder()
                        .setBooking(booking)
                        .setSectorName(sector)
                        .setCounter(counter)
                        .build();
                PassengerCheckinResponse passengerCheckinResponse = stub.passengerCheckin(passengerCheckinRequest);
                System.out.println("Booking " + booking + " for flight " + passengerCheckinResponse.getFlight() + " from "
                        + passengerCheckinResponse.getAirline() + " is now waiting to check-in on counter (" + passengerCheckinResponse.getCounters().getFrom()
                        + "-" + passengerCheckinResponse.getCounters().getTo() + ") in Sector " + passengerCheckinResponse.getSector() + " with "
                        + passengerCheckinResponse.getPassengersInQueue() + " people in line");
                break;

            case "passengerStatus":
                PassengerStatusRequest passengerStatusRequest = PassengerStatusRequest
                        .newBuilder()
                        .setBooking(booking)
                        .build();
                PassengerStatusResponse passengerStatusResponse = stub.passengerStatus(passengerStatusRequest);
                switch (passengerStatusResponse.getStatus()) {
                    case PASSENGER_STATUS_CHECKED_IN :
                        System.out.println("Booking " + booking + " for flight " + passengerStatusResponse.getFlight() + " from " + passengerStatusResponse.getAirline()
                                + " checked in at counter " + passengerStatusResponse.getCheckedInCounter() + " in Sector " + passengerStatusResponse.getSectorName());
                        break;
                    case PASSENGER_STATUS_WAITING :
                        System.out.println("Booking " + booking + " for flight " + passengerStatusResponse.getFlight() + " from " + passengerStatusResponse.getAirline()
                                + " is now waiting to check-in on counters (" + passengerStatusResponse.getCounters().getFrom() + "-" + passengerStatusResponse.getCounters().getTo()
                                + ") in Sector " + passengerStatusResponse.getSectorName() + " with " + passengerStatusResponse.getPassengersInQueue() + " people in line");
                        break;
                    case PASSENGER_STATUS_NOT_ARRIVED:
                        System.out.println("Booking " + booking + " for flight " + passengerStatusResponse.getFlight() + " from " + passengerStatusResponse.getAirline()
                                + " can check-in on counters (" + passengerStatusResponse.getCounters().getFrom() + "-" + passengerStatusResponse.getCounters().getTo()
                                + ") in Sector " + passengerStatusResponse.getSectorName());
                        break;
                }
                break;
            default:
                // TODO: Exception?
                logger.error("Unknown action: {}", action);
                break;
        }
    }
}
