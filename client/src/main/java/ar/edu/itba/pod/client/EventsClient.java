package ar.edu.itba.pod.client;

import ar.edu.itba.pod.grpc.common.CounterRange;
import ar.edu.itba.pod.grpc.events.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
        String airline;
        switch (action) {
            case "register":
                CountDownLatch countDownLatch = new CountDownLatch(1);
                airline = Optional.ofNullable(System.getProperty("airline")).orElseThrow(IllegalArgumentException::new);
                RegisterRequest registerRequest = RegisterRequest
                        .newBuilder()
                        .setAirline(airline)
                        .build();
                StreamObserver<RegisterResponse> responseObserver = new StreamObserver<RegisterResponse>() {
                    @Override
                    public void onNext(RegisterResponse registerResponse) {
                        EventType eventType = registerResponse.getEventType();
                        String sectorName;
                        CounterRange range;
                        String flight;
                        String flights;
                        String booking;
                        int counterCount;
                        int pendingAssignations;
                        switch (eventType) {
                            case EVENT_TYPE_UNSPECIFIED:
                                // TODO: idk
                                break;
                            case EVENT_TYPE_COUNTERS_ASSIGNED:
                                CountersAssignedInfo countersAssignedInfo = registerResponse.getCountersAssignedInfo();
                                range = countersAssignedInfo.getCounters();
                                counterCount = range.getTo() - range.getFrom() + 1;
                                sectorName = countersAssignedInfo.getSectorName();
                                // TODO: revisar esto
                                flights = String.join("|", countersAssignedInfo.getFlightsList());
                                System.out.println(counterCount + " counters ("+range.getFrom()+"-"+range.getTo()+") in Sector "+sectorName+" are now checking in passengers from "+airline+" "+flights+" flights");
                                break;
                            case EVENT_TYPE_COUNTERS_FREED:
                                CountersFreedInfo freedInfo = registerResponse.getCountersFreedInfo();
                                sectorName = freedInfo.getSectorName();
                                range = freedInfo.getCounters();
                                flights = String.join("|", freedInfo.getFlightsList());
                                System.out.println("Ended check-in for flights "+flights+" on counters ("+range.getFrom()+"-"+range.getTo()+") from Sector " + sectorName);
                                break;
                            case EVENT_TYPE_PASSENGER_ARRIVED:
                                PassengerArrivedInfo passengerArrivedInfo = registerResponse.getPassengerArrivedInfo();
                                booking = passengerArrivedInfo.getBooking();
                                flight = passengerArrivedInfo.getFlight();
                                sectorName = passengerArrivedInfo.getSectorName();
                                range = passengerArrivedInfo.getCounters();
                                int passengersInQueue = passengerArrivedInfo.getPassengersInQueue();
                                System.out.println("Booking "+booking+" for flight "+flight+" from "+airline+" is now waiting to check-in on counters ("+range.getFrom()+"-"+range.getTo()+") in Sector "+sectorName+" with "+passengersInQueue+" people in line");
                                break;
                            case EVENT_TYPE_PASSENGER_CHECKED_IN:
                                PassengerCheckedInInfo passengerCheckedInInfo = registerResponse.getPassengerCheckedInInfo();
                                booking = passengerCheckedInInfo.getBooking();
                                flight = passengerCheckedInInfo.getFlight();
                                sectorName = passengerCheckedInInfo.getSectorName();
                                int counter = passengerCheckedInInfo.getCounter();
                                System.out.println("Check-in successful of "+booking+" for flight "+flight+" at counter "+counter+" in Sector " + sectorName);
                                break;
                            case EVENT_TYPE_ASSIGNATION_PENDING:
                                AssignationPendingInfo assignationPendingInfo = registerResponse.getAssignationPendingInfo();
                                sectorName = assignationPendingInfo.getSectorName();
                                flights = String.join("|", assignationPendingInfo.getFlightsList());
                                counterCount = assignationPendingInfo.getCounterCount();
                                pendingAssignations = assignationPendingInfo.getPendingAssignations();
                                System.out.println(counterCount + " counters in Sector "+sectorName+" for flights "+flights+" is pending with "+pendingAssignations+" other pendings ahead");
                                break;
                            case EVENT_TYPE_MOVED_IN_ASSIGNATION_QUEUE:
                                MovedInAssignationQueueInfo movedInAssignationQueueInfo = registerResponse.getMovedInAssignationQueueInfo();
                                sectorName = movedInAssignationQueueInfo.getSectorName();
                                flights = String.join("|", movedInAssignationQueueInfo.getFlightsList());
                                counterCount = movedInAssignationQueueInfo.getCounterCount();
                                pendingAssignations = movedInAssignationQueueInfo.getPendingAssignations();
                                System.out.println(counterCount+" counters in Sector "+sectorName+" for flights "+flights+" is pending with "+pendingAssignations+" other pendings ahead");
                                break;
                            case EVENT_TYPE_AIRLINE_REGISTERED:
                                System.out.println(airline + " registered successfully for check-in events");
                                break;
                            case UNRECOGNIZED:
                                // TODO: idk
                                break;
                        }
                    }

                    // Should not happen
                    @Override
                    public void onError(Throwable throwable) {
                        // TODO: check this
                        Status status = Status.fromThrowable(throwable);
                        logger.error(status.getDescription());
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        // unblock
                        countDownLatch.countDown();
                    }
                };
                stub.register(registerRequest, responseObserver);
                // block
                try {
                    countDownLatch.wait();
                } catch (InterruptedException e) {
                    logger.error(e.getMessage());
                    return;
                }
                break;

            case "unregister":
                break;

            default:
                // TODO: Exception?
                logger.error("Unknown action: {}", action);
                break;
        }
    }
}