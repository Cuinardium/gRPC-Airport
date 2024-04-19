package ar.edu.itba.pod.client;

import ar.edu.itba.pod.client.models.Passenger;
import ar.edu.itba.pod.grpc.admin.*;
import ar.edu.itba.pod.grpc.common.CounterRange;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class AdminClient {
    private static final Logger logger = LoggerFactory.getLogger(AdminClient.class);

    public static void main(String[] args) throws InterruptedException {
        logger.info("tpe1-g4 Admin Client Starting ...");
        logger.info("grpc-com-patterns Admin Client Starting ...");

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

        AdminServiceGrpc.AdminServiceBlockingStub stub = AdminServiceGrpc.newBlockingStub(channel);

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

    private static void executeAction(String action, AdminServiceGrpc.AdminServiceBlockingStub stub) {
        switch (action) {
            case "addSector":
                String sectorName = Optional.ofNullable(System.getProperty("sector")).orElseThrow(IllegalArgumentException::new);
                AddSectorRequest addSectorRequest = AddSectorRequest
                        .newBuilder()
                        .setSectorName(sectorName)
                        .build();
                stub.addSector(addSectorRequest);
                System.out.println("Sector " + sectorName + " added successfully");
                break;

            case "addCounters":
                String sector = Optional.ofNullable(System.getProperty("sector")).orElseThrow(IllegalArgumentException::new);
                int counters = Integer.parseInt(Optional.ofNullable(System.getProperty("counters")).orElseThrow(IllegalArgumentException::new));
                AddCountersRequest addCountersRequest = AddCountersRequest
                        .newBuilder()
                        .setSectorName(sector)
                        .setCounterCount(counters)
                        .build();
                CounterRange counterRange = stub.addCounters(addCountersRequest);
                int range = counterRange.getTo() - counterRange.getFrom();
                System.out.println(range + " new counters ("+counterRange.getFrom()+"-"+counterRange.getTo()+") in Sector "+sector+" added successfully");
                break;

            case "manifest":
                String path = Optional.ofNullable(System.getProperty("inPath")).orElseThrow(IllegalArgumentException::new);
                for (Passenger passenger : parsePassengers(path)) {
                    AddPassengerRequest addPassengerRequest = AddPassengerRequest
                            .newBuilder()
                            .setBooking(passenger.booking())
                            .setFlight(passenger.flight())
                            .setAirline(passenger.airline())
                            .build();
                    stub.addPassenger(addPassengerRequest);
                    System.out.println("Booking "+passenger.booking()+" for "+passenger.airline()+" "+passenger.flight()+" added successfully");
                }
                break;
            default:
                // TODO: Exception?
                logger.error("Unknown action {}", action);
                break;
        }
    }

    private static List<Passenger> parsePassengers(String path) {
        List<Passenger> passengers = new ArrayList<>();
        Path file = Paths.get(path);
        try (BufferedReader br = new BufferedReader(new FileReader(file.toFile()))) {
            String line;
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] passengerData = line.split(";");
                passengers.add(new Passenger(passengerData[0], passengerData[1], passengerData[2]));
            }
        }catch (IOException e) {
            logger.error(e.getMessage());
        }
        return passengers;
    }
}
