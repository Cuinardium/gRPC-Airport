package ar.edu.itba.pod.client;

import ar.edu.itba.pod.grpc.common.CounterRange;
import ar.edu.itba.pod.grpc.counter.*;
import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
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
        CounterRange counterRange;
        String airline;
        List<String> flights;
        StringBuilder flightsBuilder;
        switch (action) {
            case "listSectors":
                ListSectorsResponse listSectorsResponse = stub.listSectors(Empty.getDefaultInstance());
                List<SectorInfo> sectorsList = listSectorsResponse.getSectorsList();
                System.out.println("Sectors   Counters");
                System.out.println("###################");
                for (SectorInfo sector : sectorsList) {
                    StringBuilder rangeStringBuilder = new StringBuilder();
                    if (sector.getCounterRangesList().isEmpty()) {
                        rangeStringBuilder.append("-");
                    }
                    for (CounterRange range : sector.getCounterRangesList()) {
                        rangeStringBuilder.append("(").append(range.getTo()).append("-").append(range.getFrom()).append(")");
                    }
                    System.out.printf("%-10s%-10s\n", sector.getSectorName(), rangeStringBuilder);
                }
                break;

            case "listCounters":
                sectorName = Optional.ofNullable(System.getProperty("sector")).orElseThrow(IllegalArgumentException::new);
                int from = Integer.parseInt(Optional.ofNullable(System.getProperty("counterFrom")).orElseThrow(IllegalArgumentException::new));
                int to = Integer.parseInt(Optional.ofNullable(System.getProperty("counterTo")).orElseThrow(IllegalArgumentException::new));
                counterRange = CounterRange.newBuilder().setFrom(from).setTo(to).build();
                ListCountersRequest listCountersRequest = ListCountersRequest.
                        newBuilder().
                        setSectorName(sectorName).
                        setCounterRange(counterRange).
                        build();
                ListCountersResponse listCountersResponse = stub.listCounters(listCountersRequest);
                List<CounterInfo> counterInfoList = listCountersResponse.getCountersList();
                System.out.println("Counters  Airline          Flights             People");
                System.out.println("##########################################################");
                StringBuilder flightStringBuilder;
                for (CounterInfo counterInfo : counterInfoList) {
                    counterRange = counterInfo.getCounterRange();
                    String rangeString = "(" + counterRange.getFrom() + "-" + counterRange.getTo() + ")";
                    int passengers = counterInfo.getPassengersInQueue();
                    airline = counterInfo.getAssignedAirline();
                    flightStringBuilder = new StringBuilder();
                    flights = counterInfo.getAssignedFlightsList().stream().toList();
                    for (int i = 0; i < flights.size(); ) {
                        flightStringBuilder.append(flights.get(i));
                        i++;
                        if (i < flights.size()) {
                            flightStringBuilder.append("|");
                        }
                    }
                    System.out.printf("%-10s%-18s%-20s%-8s\n", rangeString, airline, flightStringBuilder, passengers == 0 ? "-" : passengers);
                }
                break;

            case "assignCounters":
                sectorName = Optional.ofNullable(System.getProperty("sector")).orElseThrow(IllegalArgumentException::new);
                airline = Optional.ofNullable(System.getProperty("airline")).orElseThrow(IllegalArgumentException::new);
                int counterCount = Integer.parseInt(Optional.ofNullable(System.getProperty("counterCount")).orElseThrow(IllegalArgumentException::new));
                flights = Arrays.stream(Optional.ofNullable(System.getProperty("flights")).orElseThrow(IllegalArgumentException::new).split("\\|")).toList();
                CounterAssignment counterAssignment = CounterAssignment
                        .newBuilder()
                        .setCounterCount(counterCount)
                        .setAirline(airline)
                        .addAllFlights(flights)
                        .build();
                AssignCountersRequest assignCountersRequest = AssignCountersRequest
                        .newBuilder()
                        .setSectorName(sectorName)
                        .setAssignment(counterAssignment)
                        .build();
                AssignCountersResponse assignCountersResponse = stub.assignCounters(assignCountersRequest);
                AssignationStatus assignationStatus = assignCountersResponse.getStatus();
                switch (assignationStatus) {
                    case ASSIGNATION_STATUS_SUCCESSFUL:
                        counterRange = assignCountersResponse.getAssignedCounters();
                        System.out.println(counterCount + " counters (" + counterRange.getFrom() + "-" + counterRange.getTo() + ") in Sector " + sectorName + " are now checking in passengers from " + airline + " " + flights + " flights");
                        break;
                    case ASSIGNATION_STATUS_PENDING:
                        int pending = assignCountersResponse.getPendingAssignations();
                        System.out.printf(counterCount + " counters in Sector " + sectorName + " is pending with " + pending + " other pendings ahead");
                        break;
                    case ASSIGNATION_STATUS_UNSPECIFIED:
                        // TODO: idk
                        break;
                }
                break;

            case "freeCounters":
                sectorName = Optional.ofNullable(System.getProperty("sector")).orElseThrow(IllegalArgumentException::new);
                int counterFrom = Integer.parseInt(Optional.ofNullable(System.getProperty("counterFrom")).orElseThrow(IllegalArgumentException::new));
                airline = Optional.ofNullable(System.getProperty("airline")).orElseThrow(IllegalArgumentException::new);
                FreeCountersRequest freeCountersRequest = FreeCountersRequest
                        .newBuilder()
                        .setSectorName(sectorName)
                        .setCounterFrom(counterFrom)
                        .setAirline(airline)
                        .build();
                FreeCountersResponse freeCountersResponse = stub.freeCounters(freeCountersRequest);
                int freedCounters = freeCountersResponse.getFreedCounters();
                counterRange = freeCountersResponse.getCounterRange();
                flightStringBuilder = new StringBuilder();
                flights = freeCountersResponse.getFlightsList().stream().toList();
                for (int i = 0; i < flights.size(); ) {
                    flightStringBuilder.append(flights.get(i));
                    i++;
                    if (i < flights.size()) {
                        flightStringBuilder.append("|");
                    }
                }
                System.out.println("Ended check-in for flights " + flightStringBuilder + " on " + freedCounters + " counters (" + counterRange.getFrom() + "-" + counterRange.getTo() + ") in Sector " + sectorName);

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
                for (CounterAssignment assignment : assignmentsList) {
                    flightStringBuilder = new StringBuilder();
                    flights = assignment.getFlightsList().stream().toList();
                    for (int i = 0; i < flights.size(); ) {
                        flightStringBuilder.append(flights.get(i));
                        i++;
                        if (i < flights.size()) {
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
