package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.grpc.common.CounterRange;
import ar.edu.itba.pod.grpc.counter.*;
import ar.edu.itba.pod.grpc.events.EventType;
import ar.edu.itba.pod.grpc.events.PassengerCheckedInInfo;
import ar.edu.itba.pod.grpc.events.RegisterResponse;
import ar.edu.itba.pod.server.events.EventManager;
import ar.edu.itba.pod.server.exceptions.AlreadyExistsException;
import ar.edu.itba.pod.server.exceptions.HasPendingPassengersException;
import ar.edu.itba.pod.server.exceptions.UnauthorizedException;
import ar.edu.itba.pod.server.models.*;
import ar.edu.itba.pod.server.repositories.CheckinRepository;
import ar.edu.itba.pod.server.repositories.CounterRepository;
import ar.edu.itba.pod.server.repositories.PassengerRepository;

import com.google.protobuf.Empty;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.*;
import java.util.stream.Collectors;

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

        List<PendingAssignment> pendingAssignments =
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

        if (!counterRepository.hasSector(sectorName)) {
            responseObserver.onError(
                    Status.NOT_FOUND.withDescription("Sector not found").asRuntimeException());
            return;
        }

        List<CountersRange> counters;
        try{
            counters = counterRepository.freeCounters(sectorName, counterFrom, airline);
        } catch (NoSuchElementException e) {
            responseObserver.onError(
                    io.grpc.Status.NOT_FOUND
                            .withDescription(
                                    "No counters found for the specified range and airline")
                            .asRuntimeException());
            return;
        }catch (HasPendingPassengersException e) {
            responseObserver.onError(
                    Status.FAILED_PRECONDITION
                            .withDescription(
                                    "There are passengers waiting to check in in the specified counters")
                            .asRuntimeException());
            return;
        }

        // Get flights from all counters
        List<String> flights =
                counters.stream()
                        .map(CountersRange::assignedInfo)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .map(AssignedInfo::flights)
                        .flatMap(Collection::stream)
                        .toList();

        // TODO: Check CounterRange assuming ordered list
        FreeCountersResponse response =
                FreeCountersResponse.newBuilder()
                        .setCounterRange(
                                CounterRange.newBuilder()
                                        .setFrom(counters.get(0).range().from())
                                        .setTo(counters.get(counters.size() - 1).range().to())
                                        .build())
                        .setFreedCounters(counters.size())
                        .addAllFlights(flights)
                        .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void assignCounters(
            AssignCountersRequest request,
            StreamObserver<AssignCountersResponse> responseObserver) {
        String sectorName = request.getSectorName();

        if (sectorName.isEmpty()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Sector name must be provided")
                            .asRuntimeException());
            return;
        }

        Optional<Sector> maybeSector = counterRepository.getSector(sectorName);

        if (maybeSector.isEmpty()) {
            responseObserver.onError(
                    Status.NOT_FOUND.withDescription("Sector not found").asRuntimeException());
            return;
        }

        Sector sector = maybeSector.get();
        CounterAssignment counterAssignment = request.getAssignment();

        if (counterAssignment.getAirline().isEmpty()
                || counterAssignment.getFlightsList().isEmpty()
                || counterAssignment.getCounterCount() <= 0) {
            responseObserver.onError(
                    io.grpc.Status.INVALID_ARGUMENT
                            .withDescription(
                                    "Airline, flights and counter count must be provided. Counter count must be positive")
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

        // Check if there is a flight from the CounterAssignment that is already assigned to an
        // existing CountersRange
        if (sector.countersRangeList().stream()
                .filter(countersRange -> countersRange.assignedInfo().isPresent())
                .anyMatch(
                        countersRange ->
                                counterAssignment.getFlightsList().stream()
                                        .anyMatch(
                                                countersRange.assignedInfo().get().flights()
                                                        ::contains))) {
            responseObserver.onError(
                    io.grpc.Status.INVALID_ARGUMENT
                            .withDescription(
                                    "There is at least one flight from the assignment that is already assigned to a counter.")
                            .asRuntimeException());
            return;
        }

        List<PendingAssignment> queuedAssignments =
                counterRepository.getQueuedAssignments(sector.sectorName());

        // Check if there is a pending assignment that has at least one of the flights as the
        // current assignment
        if (queuedAssignments.stream()
                .anyMatch(
                        assignment ->
                                counterAssignment.getFlightsList().stream()
                                        .anyMatch(assignment.flights()::contains))) {
            responseObserver.onError(
                    io.grpc.Status.INVALID_ARGUMENT
                            .withDescription(
                                    "There is at least one flight from the assignment that is already in queue to get assigned.")
                            .asRuntimeException());
            return;
        }

        // Check if any of the flights has been previously assigned to a counter (which means that
        // it has already been checked in)
        if (counterRepository.getPreviouslyAssignedFlights().stream()
                .anyMatch(counterAssignment.getFlightsList()::contains)) {
            responseObserver.onError(
                    io.grpc.Status.INVALID_ARGUMENT
                            .withDescription(
                                    "There is at least one flight from the assignment that has already been checked in.")
                            .asRuntimeException());
            return;
        }

        AssignCountersResponse.Builder responseBuilder = AssignCountersResponse.newBuilder();

        // TODO: Check if this takes order into consideration
        Optional<CountersRange> maybeAvailableCounterRange =
                sector.countersRangeList().stream()
                        .filter(
                                range ->
                                        range.assignedInfo().isEmpty()
                                                && (range.range().to() - range.range().from())
                                                        >= counterAssignment.getCounterCount())
                        .findFirst();

        if (maybeAvailableCounterRange.isPresent()) {
            CountersRange availableCounterRange = maybeAvailableCounterRange.get();
            int from = availableCounterRange.range().from();
            int to = from + counterAssignment.getCounterCount() - 1;
            CountersRange newCountersRange =
                    new CountersRange(
                            new Range(from, to),
                            new AssignedInfo(
                                    counterAssignment.getAirline(),
                                    counterAssignment.getFlightsList(),
                                    0));
            // TODO: Check if this should be done by repository or by service
            counterRepository.assignCounterRange(sector.sectorName(), newCountersRange);
            responseBuilder
                    .setStatus(AssignationStatus.ASSIGNATION_STATUS_SUCCESSFUL)
                    .setAssignedCounters(CounterRange.newBuilder().setFrom(from).setTo(to).build());
        } else {
            counterRepository.addAssignmentToQueue(
                    request.getSectorName(),
                    new PendingAssignment(
                            counterAssignment.getAirline(),
                            counterAssignment.getFlightsList(),
                            counterAssignment.getCounterCount()));
            responseBuilder
                    .setStatus(AssignationStatus.ASSIGNATION_STATUS_PENDING)
                    .setPendingAssignations(queuedAssignments.size());
        }

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
