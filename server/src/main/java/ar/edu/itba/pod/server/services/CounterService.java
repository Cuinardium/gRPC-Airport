package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.grpc.common.CounterRange;
import ar.edu.itba.pod.grpc.counter.*;
import ar.edu.itba.pod.grpc.events.*;
import ar.edu.itba.pod.server.events.EventManager;
import ar.edu.itba.pod.server.exceptions.*;
import ar.edu.itba.pod.server.models.*;
import ar.edu.itba.pod.server.repositories.CheckinRepository;
import ar.edu.itba.pod.server.repositories.CounterRepository;
import ar.edu.itba.pod.server.repositories.PassengerRepository;

import ar.edu.itba.pod.server.utils.Pair;
import com.google.protobuf.Empty;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.*;
import java.util.stream.Collectors;

import static ar.edu.itba.pod.grpc.events.EventType.EVENT_TYPE_COUNTERS_ASSIGNED;
import static ar.edu.itba.pod.grpc.events.EventType.EVENT_TYPE_MOVED_IN_ASSIGNATION_QUEUE;

public class CounterService extends CounterServiceGrpc.CounterServiceImplBase {

    private final CounterRepository counterRepository;
    private final PassengerRepository passengerRepository;
    private final CheckinRepository checkinRepository;

    private final EventManager eventManager;

    public CounterService(
            CounterRepository counterRepository,
            PassengerRepository passengerRepository,
            CheckinRepository checkinRepository,
            EventManager eventManager) {
        this.counterRepository = counterRepository;
        this.passengerRepository = passengerRepository;
        this.checkinRepository = checkinRepository;
        this.eventManager = eventManager;
    }

    @Override
    public void listPendingAssignments(
            ListPendingAssignmentsRequest request,
            StreamObserver<ListPendingAssignmentsResponse> responseObserver) {
        String sectorName = request.getSectorName();

        if (sectorName.isEmpty()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Sector name must be provided")
                            .asRuntimeException());
            return;
        }

        if (!counterRepository.hasSector(sectorName)) {
            responseObserver.onError(
                    Status.NOT_FOUND.withDescription("Sector not found").asRuntimeException());
            return;
        }

        Queue<Assignment> pendingAssignments =
                counterRepository.getQueuedAssignments(sectorName);

        ListPendingAssignmentsResponse response =
                ListPendingAssignmentsResponse.newBuilder()
                        .addAllAssignments(
                                pendingAssignments.stream()
                                        .map(
                                                assignment ->
                                                        CounterAssignment.newBuilder()
                                                                .setAirline(assignment.airline())
                                                                .addAllFlights(assignment.flights())
                                                                .setCounterCount(
                                                                        assignment.counterCount())
                                                                .build())
                                        .toList())
                        .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void checkinCounters(
            CheckinCountersRequest request,
            StreamObserver<CheckinCountersResponse> responseObserver) {

        String sectorName = request.getSectorName();
        int counterFrom = request.getCounterFrom();
        String airline = request.getAirline();


        if (sectorName.isEmpty() || counterFrom <= 0 || airline.isEmpty()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(
                                    "Sector name, counter range (positive integer) and airline must be provided")
                            .asRuntimeException());
            return;
        }

        if (!counterRepository.hasSector(sectorName)) {
            responseObserver.onError(
                    Status.NOT_FOUND.withDescription("Sector not found").asRuntimeException());
            return;
        }

        List<Optional<String>> checkedInBookings;
        try {
            checkedInBookings = counterRepository.checkinCounters(sectorName, counterFrom, airline);
        } catch (NoSuchElementException e) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("No assigned counters found from given number")
                            .asRuntimeException());
            return;
        } catch (UnauthorizedException e) {
            responseObserver.onError(
                    Status.PERMISSION_DENIED
                            .withDescription("Counters are not assigned to the given airline")
                            .asRuntimeException());
            return;
        }

        List<Checkin> checkins = new ArrayList<>();

        int idleCounters = 0;
        for (int i = 0; i < checkedInBookings.size(); i++) {
            Optional<String> maybeBooking = checkedInBookings.get(i);
            if (maybeBooking.isEmpty()) {
                idleCounters++;
                continue;
            }

            String booking = maybeBooking.get();

            // Get flight from booking
            Passenger passenger = passengerRepository.getPassenger(booking).orElseThrow(IllegalStateException::new);
            String flight = passenger.flight();

            checkins.add(new Checkin(sectorName, counterFrom + i, airline, flight, booking));
        }

        // Add checkins to checkin repository
        for (Checkin checkin : checkins) {
            try{
                checkinRepository.addCheckin(checkin);
            } catch (AlreadyExistsException e) {
                throw new IllegalStateException("Checkin already exists");
            }
        }

        // Notify event manager
        List<RegisterResponse> checkInEvents =
                checkins.stream()
                        .map(
                                checkin ->
                                        RegisterResponse.newBuilder()
                                                .setEventType(
                                                        EventType.EVENT_TYPE_PASSENGER_CHECKED_IN)
                                                .setPassengerCheckedInInfo(
                                                        PassengerCheckedInInfo.newBuilder()
                                                                .setBooking(checkin.booking())
                                                                .setFlight(checkin.flight())
                                                                .setSectorName(sectorName)
                                                                .setCounter(checkin.counter())
                                                                .build())
                                                .build())
                        .toList();

        for (RegisterResponse checkInEvent : checkInEvents) {
            eventManager.notify(airline, checkInEvent);
        }

        CheckinCountersResponse response =
                CheckinCountersResponse.newBuilder()
                        .addAllSuccessfulCheckins(
                                checkins.stream()
                                        .map(
                                                checkin ->
                                                        CheckInInfo.newBuilder()
                                                                .setBooking(checkin.booking())
                                                                .setFlight(checkin.flight())
                                                                .setCounter(checkin.counter())
                                                                .build())
                                        .toList())
                        .setIdleCounterCount(idleCounters)
                        .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void freeCounters(
            FreeCountersRequest request, StreamObserver<FreeCountersResponse> responseObserver) {
        String sectorName = request.getSectorName();
        int counterFrom = request.getCounterFrom();
        String airline = request.getAirline();

        if (sectorName.isEmpty() || counterFrom <= 0 || airline.isEmpty()) {
            responseObserver.onError(
                    io.grpc.Status.INVALID_ARGUMENT
                            .withDescription(
                                    "Sector name, counter range (positive integer) and airline must be provided")
                            .asRuntimeException());
            return;
        }

        CountersRange freedCountersRange;
        try{
            freedCountersRange = counterRepository.freeCounters(sectorName, counterFrom, airline);
        } catch (NoSuchElementException e) {
            responseObserver.onError(
                    io.grpc.Status.NOT_FOUND
                            .withDescription(
                                    "Sector does not exist or no counters found for the specified range and airline")
                            .asRuntimeException());
            return;
        }catch (HasPendingPassengersException e) {
            responseObserver.onError(
                    Status.FAILED_PRECONDITION
                            .withDescription(
                                    "There are passengers waiting to check in in the specified counters")
                            .asRuntimeException());
            return;
        } catch (UnauthorizedException e) {
            responseObserver.onError(
                    Status.PERMISSION_DENIED
                            .withDescription("Counters are not assigned to the given airline")
                            .asRuntimeException());
            return;
        }

        Optional<AssignedInfo> maybeAssignedInfo = freedCountersRange.assignedInfo();
        if (maybeAssignedInfo.isEmpty()) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("No counters found for the specified range and airline")
                            .asRuntimeException());
            return;
        }
        List<String> flights = maybeAssignedInfo.get().flights();

        int from = freedCountersRange.range().from();
        int to = freedCountersRange.range().to();

        // TODO: Check CounterRange assuming ordered list
        FreeCountersResponse response =
                FreeCountersResponse.newBuilder()
                        .setCounterRange(
                                CounterRange.newBuilder()
                                        .setFrom(from)
                                        .setTo(to)
                                        .build())
                        .setFreedCounters(to - from + 1)
                        .addAllFlights(flights)
                        .build();

        RegisterResponse registerResponse = RegisterResponse.newBuilder()
                .setEventType(EventType.EVENT_TYPE_COUNTERS_FREED)
                .setCountersFreedInfo(CountersFreedInfo.newBuilder()
                        .setSectorName(sectorName)
                        .addAllFlights(flights)
                        .setCounters(CounterRange.newBuilder()
                                .setFrom(from)
                                .setTo(to)
                                .build())
                        .build())
                .build();

        eventManager.notify(airline, registerResponse);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void assignCounters(
            AssignCountersRequest request,
            StreamObserver<AssignCountersResponse> responseObserver) {
        String sectorName = request.getSectorName();
        CounterAssignment counterAssignment = request.getAssignment();

        if (sectorName.isEmpty()
                || counterAssignment.getAirline().isEmpty()
                || counterAssignment.getFlightsList().isEmpty()
                || counterAssignment.getCounterCount() <= 0) {
            responseObserver.onError(
                    io.grpc.Status.INVALID_ARGUMENT
                            .withDescription(
                                    "Sector name, airline, flights and counter count must be provided. Counter count must be positive")
                            .asRuntimeException());
            return;
        }

        // Get passengers for flights and map them to the corresponding flight that appear in the
        // CounterAssignment
        Map<String, List<Passenger>> passengersByFlight =
                passengerRepository.getPassengers().stream()
                        .filter(
                                passenger ->
                                        counterAssignment
                                                .getFlightsList()
                                                .contains(passenger.flight()))
                        .collect(Collectors.groupingBy(Passenger::flight));

        // Check if all flights have passengers
        boolean allFlightsHavePassengers =
                counterAssignment.getFlightsList().stream()
                        .allMatch(passengersByFlight::containsKey);

        if (!allFlightsHavePassengers) {
            responseObserver.onError(
                    io.grpc.Status.INVALID_ARGUMENT
                            .withDescription(
                                    "There are flights in the assignment that have no passengers.")
                            .asRuntimeException());
            return;
        }

        // Check if all passengers have the airline of the assignment, if not, return an error
        boolean allPassengersHaveAirline =
                passengersByFlight.values().stream()
                        .allMatch(
                                passengersList ->
                                        passengersList.stream()
                                                .allMatch(
                                                        passenger ->
                                                                passenger
                                                                        .airline()
                                                                        .equals(
                                                                                counterAssignment
                                                                                        .getAirline())));
        if (!allPassengersHaveAirline) {
            responseObserver.onError(
                    io.grpc.Status.INVALID_ARGUMENT
                            .withDescription(
                                    "There are passengers with the flight code but from another airline, for at least one of the requested flights.")
                            .asRuntimeException());
            return;
        }

        Pair<Range, Integer> assignedCounterRangeOrQueuedAssignments;
        try {
            Assignment assignment =
                    new Assignment(
                            counterAssignment.getAirline(),
                            counterAssignment.getFlightsList(),
                            counterAssignment.getCounterCount(),
                            (pending) -> {
                                MovedInAssignationQueueInfo movedInAssignationQueueInfo = MovedInAssignationQueueInfo
                                        .newBuilder()
                                        .setSectorName(sectorName)
                                        .addAllFlights(counterAssignment.getFlightsList())
                                        .setCounterCount(counterAssignment.getCounterCount())
                                        .setPendingAssignations(pending)
                                        .build();
                                RegisterResponse response = RegisterResponse
                                        .newBuilder()
                                        .setEventType(EVENT_TYPE_MOVED_IN_ASSIGNATION_QUEUE)
                                        .setMovedInAssignationQueueInfo(movedInAssignationQueueInfo)
                                        .build();
                                eventManager.notify(counterAssignment.getAirline(), response);
                                },
                            (range) -> {
                                CounterRange counterRange = CounterRange
                                        .newBuilder()
                                        .setFrom(range.from())
                                        .setTo(range.to())
                                        .build();
                                CountersAssignedInfo countersAssignedInfo = CountersAssignedInfo
                                        .newBuilder()
                                        .setSectorName(sectorName)
                                        .addAllFlights(counterAssignment.getFlightsList())
                                        .setCounters(counterRange)
                                        .build();
                                RegisterResponse response = RegisterResponse
                                        .newBuilder()
                                        .setEventType(EVENT_TYPE_COUNTERS_ASSIGNED)
                                        .setCountersAssignedInfo(countersAssignedInfo)
                                        .build();
                                eventManager.notify(counterAssignment.getAirline(), response);
                            }
                    );
            assignedCounterRangeOrQueuedAssignments =
                    counterRepository.assignCounterAssignment(sectorName, assignment);
        } catch (NoSuchElementException e) {
            responseObserver.onError(
                    io.grpc.Status.NOT_FOUND
                            .withDescription("Sector not found")
                            .asRuntimeException());
            return;
        } catch (FlightAlreadyAssignedException e) {
            responseObserver.onError(
                    Status.ALREADY_EXISTS
                            .withDescription(
                                    "There is at least one flight from the assignment that is already assigned to a counter.")
                            .asRuntimeException());
            return;
        } catch (FlightAlreadyQueuedException e) {
            responseObserver.onError(
                    Status.ALREADY_EXISTS
                            .withDescription(
                                    "There is at least one flight from the assignment that is already in queue to get assigned.")
                            .asRuntimeException());
            return;
        } catch (FlightAlreadyCheckedInException e) {
            responseObserver.onError(
                    Status.ALREADY_EXISTS
                            .withDescription(
                                    "There is at least one flight from the assignment that has already been checked in.")
                            .asRuntimeException());
            return;
        }

        AssignCountersResponse.Builder responseBuilder = AssignCountersResponse.newBuilder();

        RegisterResponse.Builder assignationEventBuilder = RegisterResponse.newBuilder();

        boolean isAssigned = assignedCounterRangeOrQueuedAssignments.second() == 0;
        if (isAssigned) {
            Range range = assignedCounterRangeOrQueuedAssignments.first();
            responseBuilder
                    .setStatus(AssignationStatus.ASSIGNATION_STATUS_SUCCESSFUL)
                    .setAssignedCounters(
                            CounterRange.newBuilder()
                                    .setFrom(range.from())
                                    .setTo(range.to())
                                    .build());

            assignationEventBuilder
                    .setEventType(EVENT_TYPE_COUNTERS_ASSIGNED)
                    .setCountersAssignedInfo(
                            CountersAssignedInfo.newBuilder()
                                    .setSectorName(sectorName)
                                    .addAllFlights(counterAssignment.getFlightsList())
                                    .setCounters(
                                            CounterRange.newBuilder()
                                                    .setFrom(range.from())
                                                    .setTo(range.to())
                                                    .build())
                                    .build());

        } else {
            int pendingAssignments = assignedCounterRangeOrQueuedAssignments.second();
            responseBuilder
                    .setStatus(AssignationStatus.ASSIGNATION_STATUS_PENDING)
                    .setPendingAssignations(assignedCounterRangeOrQueuedAssignments.second());

            assignationEventBuilder
                    .setEventType(EventType.EVENT_TYPE_ASSIGNATION_PENDING)
                    .setAssignationPendingInfo(
                            AssignationPendingInfo.newBuilder()
                                    .setSectorName(sectorName)
                                    .addAllFlights(counterAssignment.getFlightsList())
                                    .setCounterCount(counterAssignment.getCounterCount())
                                    .setPendingAssignations(pendingAssignments)
                                    .build());
        }

        eventManager.notify(counterAssignment.getAirline(), assignationEventBuilder.build());

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void listCounters(
            ListCountersRequest request, StreamObserver<ListCountersResponse> responseObserver) {

        String sectorName = request.getSectorName();
        CounterRange counterRange = request.getCounterRange();

        // Check if sector name is empty or counter range is invalid
        // Not checking if to is negative because it already needs to be greater than from
        if (sectorName.isEmpty()
                || counterRange.getFrom() <= 0
                || counterRange.getFrom() > counterRange.getTo()) {
            responseObserver.onError(
                    io.grpc.Status.INVALID_ARGUMENT
                            .withDescription(
                                    "Sector name and counter range (positive integers, from > to) must be provided")
                            .asRuntimeException());
            return;
        }

        Optional<Sector> maybeSector = counterRepository.getSector(sectorName);

        if (maybeSector.isEmpty()) {
            responseObserver.onError(
                    io.grpc.Status.NOT_FOUND
                            .withDescription("Sector not found")
                            .asRuntimeException());
            return;
        }

        Sector sector = maybeSector.get();

        ListCountersResponse.Builder responseBuilder = ListCountersResponse.newBuilder();

        for (CountersRange countersRange : sector.countersRangeList()) {
            if (countersRange.range().from() >= counterRange.getFrom()
                    && countersRange.range().to() <= counterRange.getTo()) {
                CounterInfo.Builder counterInfoBuilder = CounterInfo.newBuilder();

                counterInfoBuilder.setCounterRange(
                        CounterRange.newBuilder()
                                .setFrom(countersRange.range().from())
                                .setTo(countersRange.range().to())
                                .build());

                Optional<AssignedInfo> assignedInfo = countersRange.assignedInfo();
                if (assignedInfo.isPresent()) {
                    AssignedInfo info = assignedInfo.get();
                    counterInfoBuilder
                            .setAssignedAirline(info.airline())
                            .addAllAssignedFlights(info.flights())
                            .setPassengersInQueue(info.passengersInQueue());
                }

                responseBuilder.addCounters(counterInfoBuilder.build());
            }
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void listSectors(Empty request, StreamObserver<ListSectorsResponse> responseObserver) {
        ListSectorsResponse.Builder responseBuilder = ListSectorsResponse.newBuilder();

        List<Sector> sectors = counterRepository.getSectors();

        if (sectors.isEmpty()) {
            responseObserver.onError(
                    io.grpc.Status.NOT_FOUND
                            .withDescription("No sectors found")
                            .asRuntimeException());
            return;
        }

        for (Sector sector : sectors) {

            // Contiguous ranges should be merged
            List<Range> mergedRanges =
                    Range.mergeRanges(
                            sector.countersRangeList().stream()
                                    .map(CountersRange::range)
                                    .collect(Collectors.toList()));

            // Mapping from model to proto CounterRange
            List<CounterRange> counterRangesList =
                    mergedRanges.stream()
                            .map(
                                    (range ->
                                            CounterRange.newBuilder()
                                                    .setFrom(range.from())
                                                    .setTo(range.to())
                                                    .build()))
                            .collect(Collectors.toList());

            // Create SectorInfo with provided sector
            SectorInfo sectorInfo =
                    SectorInfo.newBuilder()
                            .setSectorName(sector.sectorName())
                            .addAllCounterRanges(counterRangesList)
                            .build();
            responseBuilder.addSectors(sectorInfo);
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }
}
