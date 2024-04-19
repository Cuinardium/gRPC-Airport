package ar.edu.itba.pod.client;

import ar.edu.itba.pod.grpc.common.CounterRange;
import ar.edu.itba.pod.grpc.counter.*;
import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class CounterClient {
    private static final Logger logger = LoggerFactory.getLogger(CounterClient.class);

    public static void main(String[] args) throws InterruptedException {
        logger.info("tpe1-g4 Counter Client Starting ...");
        logger.info("grpc-com-patterns Counter Client Starting ...");


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

        CounterServiceGrpc.CounterServiceBlockingStub stub = CounterServiceGrpc.newBlockingStub(channel);

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

    private static void executeAction(String action, CounterServiceGrpc.CounterServiceBlockingStub stub) {
        String sectorName;
        switch (action) {
            case "listSectors":
                ListSectorsResponse listSectorsResponse = stub.listSectors(Empty.getDefaultInstance());
                List<SectorInfo> sectorsList = listSectorsResponse.getSectorsList();
                System.out.println("Sectors   Counters");
                System.out.println("###################");
                for(SectorInfo sector : sectorsList) {
                    StringBuilder rangeStringBuilder = new StringBuilder();
                    if(sector.getCounterRangesList().isEmpty()) {
                        rangeStringBuilder.append("-");
                    }
                    for(CounterRange range : sector.getCounterRangesList()) {
                        rangeStringBuilder.append("(").append(range.getTo()).append("-").append(range.getFrom()).append(")");
                    }
                    System.out.printf("%-10s%-10s\n", sector.getSectorName(), rangeStringBuilder);
                }
                break;

            case "listCounters":
                sectorName = Optional.ofNullable(System.getProperty("sector")).orElseThrow(IllegalArgumentException::new);
                int from = Integer.parseInt(Optional.ofNullable(System.getProperty("counterFrom")).orElseThrow(IllegalArgumentException::new));
                int to = Integer.parseInt(Optional.ofNullable(System.getProperty("counterTo")).orElseThrow(IllegalArgumentException::new));
                CounterRange range = CounterRange.newBuilder().setFrom(from).setTo(to).build();
                ListCountersRequest listCountersRequest = ListCountersRequest.
                        newBuilder().
                        setSectorName(sectorName).
                        setCounterRange(range).
                        build();
                ListCountersResponse listCountersResponse = stub.listCounters(listCountersRequest);
                List<CounterInfo> counterInfoList = listCountersResponse.getCountersList();
                System.out.println("Counters  Airline          Flights             People");
                System.out.println("##########################################################");
                for(CounterInfo counterInfo : counterInfoList) {
                    range = counterInfo.getCounterRange();
                    String rangeString = "("+range.getFrom()+"-"+range.getTo()+")";
                    int passengers = counterInfo.getPassengersInQueue();
                    String airline = counterInfo.getAssignedAirline();
                    StringBuilder flightStringBuilder = new StringBuilder();
                    List<String> flights = counterInfo.getAssignedFlightsList().stream().toList();
                    for(int i = 0; i < flights.size();) {
                        flightStringBuilder.append(flights.get(i));
                        i++;
                        if(i < flights.size()) {
                            flightStringBuilder.append("|");
                        }
                    }
                    System.out.printf("%-10s%-18s%-20s%-8s\n", rangeString, airline, flightStringBuilder, passengers == 0 ? "-":passengers);
                }
                break;

            case "assignCounters":

                break;

            case "freeCounters":

                break;

            case "checkinCounters":

                break;

            case "listPendingAssignments":
                sectorName = Optional.ofNullable(System.getProperty("sector")).orElseThrow(IllegalArgumentException::new);
                ListPendingAssignmentsRequest listPendingAssignmentsRequest = ListPendingAssignmentsRequest
                        .newBuilder()
                        .setSectorName(sectorName)
                        .build();
                ListPendingAssignmentsResponse listPendingAssignmentsResponse = stub.listPendingAssignments(listPendingAssignmentsRequest);
                List<CounterAssignment> assignmentsList = listPendingAssignmentsResponse.getAssignmentsList();
                System.out.println("Counters  Airline          Flights");
                System.out.println("##########################################################");
                for(CounterAssignment assignment : assignmentsList) {
                    StringBuilder flightStringBuilder = new StringBuilder();
                    List<String> flights = assignment.getFlightsList().stream().toList();
                    for(int i = 0; i < flights.size();) {
                        flightStringBuilder.append(flights.get(i));
                        i++;
                        if(i < flights.size()) {
                            flightStringBuilder.append("|");
                        }
                    }
                    System.out.printf("%-10s%-18s%-12s\n", assignment.getCounterCount(), assignment.getAirline(), flightStringBuilder);
                }

                break;
            default:
                // TODO: Exception?
                logger.error("Unknown action: {}", action);
                break;
        }
    }
}
