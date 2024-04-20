package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.grpc.common.CounterRange;
import ar.edu.itba.pod.grpc.counter.*;
import ar.edu.itba.pod.server.events.EventManager;
import ar.edu.itba.pod.server.models.CountersRange;
import ar.edu.itba.pod.server.models.Range;
import ar.edu.itba.pod.server.models.Sector;
import ar.edu.itba.pod.server.repositories.CheckinRepository;
import ar.edu.itba.pod.server.repositories.CounterRepository;
import ar.edu.itba.pod.server.repositories.PassengerRepository;
import com.google.protobuf.Empty;

import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            FreeCountersRequest request, StreamObserver<FreeCountersResponse> responseObserver) {}

    @Override
    public void assignCounters(
            AssignCountersRequest request,
            StreamObserver<AssignCountersResponse> responseObserver) {}

    @Override
    public void listCounters(
            ListCountersRequest request, StreamObserver<ListCountersResponse> responseObserver) {}

    @Override
    public void listSectors(
            Empty request, StreamObserver<ListSectorsResponse> responseObserver) {
        ListSectorsResponse.Builder responseBuilder = ListSectorsResponse.newBuilder();

        for (Sector sector : counterRepository.getSectors()) {
            // Mapping from model to proto CounterRange
            List<CounterRange> counterRangesList = sector.countersRangeList().stream().map((countersRange -> {
                Range range = countersRange.range();
                return CounterRange.newBuilder()
                        .setFrom(range.from())
                        .setTo(range.to())
                        .build();
            })).collect(Collectors.toList());

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
