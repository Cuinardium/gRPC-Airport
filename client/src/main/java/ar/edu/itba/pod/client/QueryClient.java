package ar.edu.itba.pod.client;

import ar.edu.itba.pod.grpc.query.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class QueryClient {
    private static final Logger logger = LoggerFactory.getLogger(QueryClient.class);

    public static void main(String[] args) throws InterruptedException {
        logger.info("tpe1-g4 Query Client Starting ...");
        
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

        QueryServiceGrpc.QueryServiceBlockingStub stub = QueryServiceGrpc.newBlockingStub(channel);

        try {
            action = Optional.ofNullable(System.getProperty("action")).orElseThrow(IllegalArgumentException::new);
        } catch (IllegalArgumentException e) {
            logger.error("You must specify an action");
            return;
        }
        try {
            executeAction(action, stub);
        } catch (IOException e){
            logger.error(e.getMessage());
        }
        finally {
            channel.shutdown().awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private static void executeAction(String action, QueryServiceGrpc.QueryServiceBlockingStub stub) throws IOException {
        String outPath = Optional.ofNullable(System.getProperty("outPath")).orElseThrow(IllegalArgumentException::new);
        switch (action) {
            case "counters":

                CountersRequest countersRequestEmpty = CountersRequest.newBuilder().build();
                try {
                    CountersResponse countersResponseEmpty = stub.counters(countersRequestEmpty);
                    outputCountersFile(outPath, countersResponseEmpty.getCountersList());
                }catch (RuntimeException e){
                    Status status= Status.fromThrowable(e);
                    System.out.println("Error: " + status.getDescription());
                }
                break;

            case "queryCounters":
                String sectorCounter = Optional.ofNullable(System.getProperty("sector")).orElseThrow(IllegalArgumentException::new);
                CountersRequest countersRequest = CountersRequest
                        .newBuilder()
                        .setSectorName(sectorCounter)
                        .build();
                try {
                    CountersResponse countersResponse = stub.counters(countersRequest);
                    outputCountersFile(outPath, countersResponse.getCountersList());
                }catch (RuntimeException e){
                    Status status= Status.fromThrowable(e);
                    System.out.println("Error: " + status.getDescription());
                }
                break;

            case "checkins":
                Optional<String> sectorCheckin = Optional.ofNullable(System.getProperty("sector"));
                Optional<String> airline= Optional.ofNullable(System.getProperty("airline"));
                CheckinsRequest checkinsRequest = CheckinsRequest
                        .newBuilder()
                        .setSectorName(sectorCheckin.orElse(null))
                        .setAirline(airline.orElse(null))
                        .build();
                try {
                    CheckinsResponse checkinsResponse = stub.checkins(checkinsRequest);
                    outputCheckinsFile(outPath, checkinsResponse.getCheckinsList());
                }catch (RuntimeException e){
                    Status status= Status.fromThrowable(e);
                    System.out.println("Error: " + status.getDescription());
                }
                break;

            default:
                // TODO: Exception?
                logger.error("Unknown action: {}", action);
                break;
        }

    }

    private static void outputCountersFile(String fileName, List<CountersInfo> countersList) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
        writer.append("Sector  Counters  Airline         Flights             People\n");
        writer.append("###########################################################\n");
        for (CountersInfo countersInfo : countersList) {
            writer.append(String.format("%-8s%-10s%-17s",
                    countersInfo.getSectorName(),
                    "(" + countersInfo.getCounters().getFrom() + "-" + countersInfo.getCounters().getTo() + ")",
                    countersInfo.getAirline()));
            StringBuilder flightStringBuilder = new StringBuilder();
            int i =1;
            for (String flight: countersInfo.getFlightsList()){
                flightStringBuilder.append(String.format("%s%s", flight, i< countersInfo.getFlightsCount()? "|": ""));
                i++;
            }
            writer.append(String.format("%-20s%-6s\n",flightStringBuilder, countersInfo.getPassengersInQueue()));
        }
        writer.close();

    }

    private static void outputCheckinsFile(String fileName, List<CheckinInfo> checkinsList) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
        writer.append("Sector  Counter   Airline           Flight     Booking\n");
        writer.append("###########################################################\n");
        for (CheckinInfo checkinInfo : checkinsList) {
            writer.append(String.format("%-8s%-10d%-18s%-11s%-7s\n",
                    checkinInfo.getSectorName(),
                    checkinInfo.getCounter(),
                    checkinInfo.getAirline(),
                    checkinInfo.getFlight(),
                    checkinInfo.getBooking()));
        }
        writer.close();

    }
}
