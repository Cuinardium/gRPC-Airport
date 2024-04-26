package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.grpc.common.CounterRange;
import ar.edu.itba.pod.grpc.counter.*;
import ar.edu.itba.pod.server.events.EventManager;
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

    }

    @Override
    public void checkinCounters(
            CheckinCountersRequest request,
            StreamObserver<CheckinCountersResponse> responseObserver) {

    }

    @Override
    public void freeCounters(
            FreeCountersRequest request, StreamObserver<FreeCountersResponse> responseObserver) {
    }

    @Override
    public void assignCounters(
            AssignCountersRequest request,
            StreamObserver<AssignCountersResponse> responseObserver) {
        Optional<Sector> maybeSector = counterRepository.getSector(request.getSectorName());

        if (maybeSector.isEmpty()) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("Sector not found")
                            .asRuntimeException());
            return;
        }

        Sector sector = maybeSector.get();
        CounterAssignment counterAssignment = request.getAssignment();

        if(
                counterAssignment.getAirline().isEmpty() ||
                counterAssignment.getFlightsList().isEmpty() ||
                counterAssignment.getCounterCount() <= 0
        ){
            responseObserver.onError(
                    io.grpc.Status.INVALID_ARGUMENT
                            .withDescription("Airline, flights and counter count must be provided. Counter count must be positive")
                            .asRuntimeException());
            return;
        }

        // Get passengers for flights and map them to the corresponding flight that appear in the CounterAssignment
        Map<String, List<Passenger>> passengersByFlight =
                passengerRepository.getPassengers().stream()
                        .filter(passenger -> counterAssignment.getFlightsList().contains(passenger.flight()))
                        .collect(Collectors.groupingBy(Passenger::flight));

        // Check if all passengers have the airline of the assignment, if not, return an error
        if(
                passengersByFlight
                        .values()
                        .stream()
                        .anyMatch(
                                passengersList -> passengersList
                                                    .stream()
                                                    .anyMatch(passenger -> !passenger.airline().equals(counterAssignment.getAirline()))
                        )
        ){
            responseObserver.onError(
                    io.grpc.Status.INVALID_ARGUMENT
                            .withDescription("There are passengers with the flight code but from another airline, for at least one of the requested flights.")
                            .asRuntimeException());
            return;
        }

        // Check if there is a flight from the CounterAssignment that is already assigned to an existing CountersRange
        if(
                sector.countersRangeList()
                        .stream()
                        .filter(countersRange -> countersRange.assignedInfo().isPresent())
                        .anyMatch(
                                countersRange -> counterAssignment.getFlightsList().stream().anyMatch(countersRange.assignedInfo().get().flights()::contains)
                        )
        ){
            responseObserver.onError(
                    io.grpc.Status.INVALID_ARGUMENT
                            .withDescription("There is at least one flight from the assignment that is already assigned to a counter.")
                            .asRuntimeException());
            return;
        }

        List<CounterAssignment> queuedAssignments = counterRepository.getQueuedAssignments(sector.sectorName());

        // Check if there is a pending assignment that has at least one of the flights as the current assignment
        if(
                queuedAssignments
                        .stream()
                        .anyMatch(
                                assignment -> counterAssignment.getFlightsList().stream().anyMatch(assignment.getFlightsList()::contains)
                        )
        ){
            responseObserver.onError(
                    io.grpc.Status.INVALID_ARGUMENT
                            .withDescription("There is at least one flight from the assignment that is already in queue to get assigned.")
                            .asRuntimeException());
            return;
        }

        //Check if any of the flights has been previously assigned to a counter (which means that it has already been checked in)
        if(
                counterRepository.getPreviouslyAssignedFlights()
                        .stream()
                        .anyMatch(counterAssignment.getFlightsList()::contains)
        ){
            responseObserver.onError(
                    io.grpc.Status.INVALID_ARGUMENT
                            .withDescription("There is at least one flight from the assignment that has already been checked in.")
                            .asRuntimeException());
            return;
        }

        AssignCountersResponse.Builder responseBuilder = AssignCountersResponse.newBuilder();

        // TODO: Check if this takes order into consideration
        Optional<CountersRange> maybeAvailableCounterRange = sector.countersRangeList().stream()
                .filter(range -> range.assignedInfo().isEmpty() && (range.range().to() - range.range().from()) >= counterAssignment.getCounterCount())
                .findFirst();

        if (maybeAvailableCounterRange.isPresent()) {
            CountersRange availableCounterRange = maybeAvailableCounterRange.get();
            int from = availableCounterRange.range().from();
            int to = from + counterAssignment.getCounterCount() - 1;
            CountersRange newCountersRange = new CountersRange(
                    new Range(from, to),
                    new AssignedInfo(
                            counterAssignment.getAirline(),
                            counterAssignment.getFlightsList(),
                            0));
            //TODO: Check if this should be done by repository or by service
            counterRepository.assignCounterRange(sector.sectorName(), newCountersRange);
            responseBuilder
                    .setStatus(AssignationStatus.ASSIGNATION_STATUS_SUCCESSFUL)
                    .setAssignedCounters(
                            CounterRange.newBuilder()
                                    .setFrom(from)
                                    .setTo(to)
                                    .build());
        } else {
            counterRepository.addAssignmentToQueue(request.getSectorName(), counterAssignment);
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
        if (sectorName.isEmpty() || counterRange.getFrom() <= 0 || counterRange.getFrom() > counterRange.getTo()) {
            responseObserver.onError(
                    io.grpc.Status.INVALID_ARGUMENT
                            .withDescription("Sector name and counter range (positive integers, from > to) must be provided")
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
            if (countersRange.range().from() >= counterRange.getFrom() && countersRange.range().to() <= counterRange.getTo()) {
                CounterInfo.Builder counterInfoBuilder = CounterInfo.newBuilder();

                counterInfoBuilder
                        .setCounterRange(
                                CounterRange
                                        .newBuilder()
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
    public void listSectors(
            Empty request, StreamObserver<ListSectorsResponse> responseObserver) {
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
            List<Range> mergedRanges = Range.mergeRanges(
                    sector.countersRangeList().stream()
                            .map(CountersRange::range)
                            .collect(Collectors.toList()));

            // Mapping from model to proto CounterRange
            List<CounterRange> counterRangesList = mergedRanges.stream().map((range ->
                CounterRange.newBuilder()
                        .setFrom(range.from())
                        .setTo(range.to())
                        .build()
            )).collect(Collectors.toList());

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
