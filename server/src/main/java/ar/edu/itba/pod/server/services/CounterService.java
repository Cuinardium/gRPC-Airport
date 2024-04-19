package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.grpc.common.CounterRange;
import ar.edu.itba.pod.grpc.counter.*;
import ar.edu.itba.pod.server.events.EventManager;
import ar.edu.itba.pod.server.models.CountersRange;
import ar.edu.itba.pod.server.models.Range;
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
        List<CountersRange> countersRanges = counterRepository.getCounters();

        List<SectorInfo> sectorsInfoList = generateSectorInfoList(countersRanges);

        ListSectorsResponse response =
                ListSectorsResponse.newBuilder()
                        .addAllSectors(sectorsInfoList)
                        .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
    //TODO: REVISE THIS
    private List<SectorInfo> generateSectorInfoList(List<CountersRange> countersRanges) {
        Map<String, List<CountersRange>> sectorsMap = new HashMap<>();

        for (CountersRange countersRange : countersRanges) {
            String sector = countersRange.sector();
            if (!sectorsMap.containsKey(sector)) {
                sectorsMap.put(sector, new ArrayList<>());
            }
            sectorsMap.get(sector).add(countersRange);
        }

        List<SectorInfo> sectorInfoList = new ArrayList<>();
        for(Map.Entry<String, List<CountersRange>> entry : sectorsMap.entrySet()) {
            String sector = entry.getKey();
            List<CounterRange> counterRangesList = entry.getValue().stream().map((countersRange -> {
                Range range = countersRange.range();
                return CounterRange.newBuilder()
                                .setFrom(range.from())
                                .setTo(range.to())
                                .build();
            })).collect(Collectors.toList());

            SectorInfo sectorInfo =
                    SectorInfo.newBuilder()
                            .setSectorName(sector)
                            .addAllCounterRanges(counterRangesList)
                            .build();
            sectorInfoList.add(sectorInfo);
        }
        return sectorInfoList;
    }
}
