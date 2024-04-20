package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.grpc.common.CounterRange;
import ar.edu.itba.pod.grpc.query.*;
import ar.edu.itba.pod.server.models.Checkin;
import ar.edu.itba.pod.server.models.CountersRange;
import ar.edu.itba.pod.server.models.Sector;
import ar.edu.itba.pod.server.repositories.CheckinRepository;
import ar.edu.itba.pod.server.repositories.CounterRepository;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class QueryService extends QueryServiceGrpc.QueryServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(QueryService.class);

    private final CounterRepository counterRepository;
    private final CheckinRepository checkinRepository;

    public QueryService(CounterRepository counterRepository, CheckinRepository checkinRepository) {
        this.counterRepository = counterRepository;
        this.checkinRepository = checkinRepository;
    }

    @Override
    public void checkins(
            CheckinsRequest request, StreamObserver<CheckinsResponse> responseObserver) {

        logger.debug("Received checkins request");

        if (!checkinRepository.hasCheckins()) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("No checkins have been registered")
                            .asRuntimeException());

            logger.debug("(queryService/checkins) request failed: no checkins have been registered");

            return;
        }

        String sector = request.getSectorName();
        String airline = request.getAirline();

        Predicate<Checkin> predicate = checkin -> true;

        if (!sector.isEmpty()) {
            predicate = predicate.and(checkin -> checkin.sector().equals(sector));

            logger.debug("(queryService/checkins) filtering checkins by sector: {}", sector);
        }

        if (!airline.isEmpty()) {
            predicate = predicate.and(checkin -> checkin.airline().equals(airline));

            logger.debug("(queryService/checkins) filtering checkins by airline: {}", airline);
        }

        List<CheckinInfo> checkins =
                checkinRepository.getCheckins(predicate).stream()
                        .map(
                                checkin ->
                                        CheckinInfo.newBuilder()
                                                .setSectorName(checkin.sector())
                                                .setCounter(checkin.counter())
                                                .setAirline(checkin.airline())
                                                .setFlight(checkin.flight())
                                                .setBooking(checkin.booking())
                                                .build())
                        .toList();

        CheckinsResponse response = CheckinsResponse.newBuilder().addAllCheckins(checkins).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        logger.debug("(queryService/checkins) checkins request completed successfully");
    }

    @Override
    public void counters(
            CountersRequest request, StreamObserver<CountersResponse> responseObserver) {

        logger.debug("(queryService/counters) Received counters request");

        if (!counterRepository.hasCounters()) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("No counters have been added to this airport")
                            .asRuntimeException());

            logger.debug("(queryService/counters) counters request failed: no counters have been added to this airport");

            return;
        }

        String sectorName = request.getSectorName();

        List<Sector> sectors = new ArrayList<>();

        if (!sectorName.isEmpty()) {
            sectors.add(counterRepository.getSector(sectorName));
            logger.debug("(queryService/counters) filtering counters by sector: {}", sectorName);
        }
        else{
            sectors = counterRepository.getSectors();
        }

        CountersResponse.Builder responseBuilder = CountersResponse.newBuilder();

        for (Sector sector : sectors){
            List<CountersInfo> counters =
                    sector.countersRangeList().stream()
                            .map(counterRange -> mapCountersRangeToCountersInfo(counterRange, sector.sectorName()))
                            .toList();

            responseBuilder.addAllCounters(counters);
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();

        logger.debug("(queryService/counters) counters request completed successfully");
    }

    private CountersInfo mapCountersRangeToCountersInfo(CountersRange countersRange, String sectorName) {
        CountersInfo.Builder countersInfoBuilder =
                CountersInfo.newBuilder()
                        .setSectorName(sectorName)
                        .setCounters(
                                CounterRange.newBuilder()
                                        .setFrom(countersRange.range().from())
                                        .setTo(countersRange.range().to())
                                        .build());

        countersRange
                .assignedInfo()
                .ifPresent(
                        assignedInfo ->
                                countersInfoBuilder
                                        .setAirline(assignedInfo.airline())
                                        .addAllFlights(assignedInfo.flights())
                                        .setPassengersInQueue(assignedInfo.passengersInQueue()));

        return countersInfoBuilder.build();
    }
}
