package ar.edu.itba.pod.server.services;

import static org.mockito.Mockito.*;

import ar.edu.itba.pod.grpc.common.CounterRange;
import ar.edu.itba.pod.grpc.counter.CounterServiceGrpc;
import ar.edu.itba.pod.grpc.counter.ListCountersRequest;
import ar.edu.itba.pod.grpc.counter.ListCountersResponse;
import ar.edu.itba.pod.grpc.counter.ListSectorsResponse;
import ar.edu.itba.pod.server.events.EventManager;
import ar.edu.itba.pod.server.models.AssignedInfo;
import ar.edu.itba.pod.server.models.CountersRange;
import ar.edu.itba.pod.server.models.Range;
import ar.edu.itba.pod.server.models.Sector;
import ar.edu.itba.pod.server.repositories.CheckinRepository;
import ar.edu.itba.pod.server.repositories.CounterRepository;
import ar.edu.itba.pod.server.repositories.PassengerRepository;

import com.google.protobuf.Empty;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RunWith(JUnit4.class)
public class CounterServiceTest {

    @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    private final CounterRepository counterRepository = mock(CounterRepository.class);
    private final PassengerRepository passengerRepository = mock(PassengerRepository.class);
    private final CheckinRepository checkinRepository = mock(CheckinRepository.class);
    private final EventManager eventManager = mock(EventManager.class);

    private final AssignedInfo sector23AssignedInfo =
            new AssignedInfo("AmericanAirlines", List.of("AA123", "AA124", "AA125"), 6);

    private final List<CountersRange> sectorDCounters = List.of(new CountersRange(new Range(5, 6)));
    private final List<CountersRange> sectorACounters = List.of(new CountersRange(new Range(1, 1)));
    private final List<CountersRange> sectorCCounters =
            List.of(
                    new CountersRange(new Range(2, 3), sector23AssignedInfo),
                    new CountersRange(new Range(4, 4)),
                    new CountersRange(new Range(7, 8)));
    private final List<Sector> sectors =
            List.of(
                    new Sector("A", sectorACounters),
                    new Sector("C", sectorCCounters),
                    new Sector("D", sectorDCounters),
                    new Sector("Z", Collections.emptyList()));
    private CounterServiceGrpc.CounterServiceBlockingStub blockingStub;

    @Before
    public void setUp() throws Exception {
        String serverName = InProcessServerBuilder.generateName();

        CounterService counterService =
                new CounterService(
                        counterRepository, passengerRepository, checkinRepository, eventManager);

        grpcCleanup.register(
                InProcessServerBuilder.forName(serverName)
                        .directExecutor()
                        .addService(counterService)
                        .build()
                        .start());

        blockingStub =
                CounterServiceGrpc.newBlockingStub(
                        grpcCleanup.register(
                                InProcessChannelBuilder.forName(serverName)
                                        .directExecutor()
                                        .build()));
    }

    @Test
    public void testListSectorsNoSectors() {
        when(counterRepository.getSectors()).thenReturn(Collections.emptyList());

        StatusRuntimeException exception =
                Assertions.assertThrows(
                        StatusRuntimeException.class,
                        () -> blockingStub.listSectors(Empty.getDefaultInstance()));

        Assertions.assertEquals(Status.NOT_FOUND.getCode(), exception.getStatus().getCode());
        Assertions.assertEquals("No sectors found", exception.getStatus().getDescription());
    }

    @Test
    public void testListSectors() {
        when(counterRepository.getSectors()).thenReturn(sectors);

        ListSectorsResponse response = blockingStub.listSectors(Empty.getDefaultInstance());

        Assertions.assertEquals(sectors.size(), response.getSectorsCount());

        for (int i = 0; i < sectors.size(); i++) {
            Assertions.assertEquals(
                    sectors.get(i).sectorName(), response.getSectors(i).getSectorName());

            // Contiguous ranges should be merged
            // e.g. [(2,3), (4,4), (7,8)] -> [(2,4), (7,8)]
            List<Range> expectedList =
                    Range.mergeRanges(
                            sectors.get(i).countersRangeList().stream()
                                    .map(CountersRange::range)
                                    .toList());

            List<CounterRange> actualList = response.getSectors(i).getCounterRangesList();

            Assertions.assertEquals(expectedList.size(), actualList.size());

            for (int j = 0; j < expectedList.size(); j++) {
                Assertions.assertEquals(expectedList.get(j).from(), actualList.get(j).getFrom());
                Assertions.assertEquals(expectedList.get(j).to(), actualList.get(j).getTo());
            }
        }
    }

    @Test
    public void testListCountersNoSectorName() {
        StatusRuntimeException exception =
                Assertions.assertThrows(
                        StatusRuntimeException.class,
                        () ->
                                blockingStub.listCounters(
                                        ListCountersRequest.newBuilder()
                                                .setCounterRange(
                                                        CounterRange.newBuilder()
                                                                .setFrom(2)
                                                                .setTo(5)
                                                                .build())
                                                .build()));

        Assertions.assertEquals(Status.INVALID_ARGUMENT.getCode(), exception.getStatus().getCode());
        Assertions.assertEquals(
                "Sector name and counter range (positive integers, from > to) must be provided",
                exception.getStatus().getDescription());
    }

    @Test
    public void testListCountersNoCounterRange() {
        StatusRuntimeException exception =
                Assertions.assertThrows(
                        StatusRuntimeException.class,
                        () ->
                                blockingStub.listCounters(
                                        ListCountersRequest.newBuilder()
                                                .setSectorName("A")
                                                .build()));

        Assertions.assertEquals(Status.INVALID_ARGUMENT.getCode(), exception.getStatus().getCode());
        Assertions.assertEquals(
                "Sector name and counter range (positive integers, from > to) must be provided",
                exception.getStatus().getDescription());
    }

    @Test
    public void testListCountersInvalidCounterRange() {
        StatusRuntimeException exception =
                Assertions.assertThrows(
                        StatusRuntimeException.class,
                        () ->
                                blockingStub.listCounters(
                                        ListCountersRequest.newBuilder()
                                                .setSectorName("A")
                                                .setCounterRange(
                                                        CounterRange.newBuilder()
                                                                .setFrom(5)
                                                                .setTo(2)
                                                                .build())
                                                .build()));

        Assertions.assertEquals(Status.INVALID_ARGUMENT.getCode(), exception.getStatus().getCode());
        Assertions.assertEquals(
                "Sector name and counter range (positive integers, from > to) must be provided",
                exception.getStatus().getDescription());
    }

    @Test
    public void testListCountersNegativeCounterRange() {
        StatusRuntimeException exception =
                Assertions.assertThrows(
                        StatusRuntimeException.class,
                        () ->
                                blockingStub.listCounters(
                                        ListCountersRequest.newBuilder()
                                                .setSectorName("A")
                                                .setCounterRange(
                                                        CounterRange.newBuilder()
                                                                .setFrom(-1)
                                                                .setTo(2)
                                                                .build())
                                                .build()));

        Assertions.assertEquals(Status.INVALID_ARGUMENT.getCode(), exception.getStatus().getCode());
        Assertions.assertEquals(
                "Sector name and counter range (positive integers, from > to) must be provided",
                exception.getStatus().getDescription());
    }

    @Test
    public void testListCountersSectorNotFound() {
        when(counterRepository.getSector("A")).thenReturn(Optional.empty());

        StatusRuntimeException exception =
                Assertions.assertThrows(
                        StatusRuntimeException.class,
                        () ->
                                blockingStub.listCounters(
                                        ListCountersRequest.newBuilder()
                                                .setSectorName("A")
                                                .setCounterRange(
                                                        CounterRange.newBuilder()
                                                                .setFrom(2)
                                                                .setTo(5)
                                                                .build())
                                                .build()));

        Assertions.assertEquals(Status.NOT_FOUND.getCode(), exception.getStatus().getCode());
        Assertions.assertEquals("Sector not found", exception.getStatus().getDescription());
    }

    @Test
    public void testListCounters() {
        Sector sectorC = sectors.get(1);
        when(counterRepository.getSector("C")).thenReturn(Optional.of(sectorC));

        ListCountersRequest request =
                ListCountersRequest.newBuilder()
                        .setSectorName("C")
                        .setCounterRange(CounterRange.newBuilder().setFrom(2).setTo(5).build())
                        .build();

        List<CountersRange> expectedCounters = sectorC.countersRangeList().subList(0, 2);

        ListCountersResponse response = blockingStub.listCounters(request);

        Assertions.assertEquals(expectedCounters.size(), response.getCountersList().size());

        for (int i = 0; i < expectedCounters.size(); i++) {
            Assertions.assertEquals(
                    expectedCounters.get(i).range().from(),
                    response.getCounters(i).getCounterRange().getFrom());
            Assertions.assertEquals(
                    expectedCounters.get(i).range().to(),
                    response.getCounters(i).getCounterRange().getTo());

            if (expectedCounters.get(i).assignedInfo().isPresent()) {
                AssignedInfo expectedAssignedInfo = expectedCounters.get(i).assignedInfo().get();
                Assertions.assertEquals(
                        expectedAssignedInfo.airline(),
                        response.getCounters(i).getAssignedAirline());
                Assertions.assertEquals(
                        expectedAssignedInfo.flights(),
                        response.getCounters(i).getAssignedFlightsList());
                Assertions.assertEquals(
                        expectedAssignedInfo.passengersInQueue(),
                        (response.getCounters(i).getPassengersInQueue()));
            }
        }
    }

    @Test
    public void testListCountersNoCountersInRange() {
        Sector sectorC = sectors.get(1);
        when(counterRepository.getSector("C")).thenReturn(Optional.of(sectorC));

        ListCountersRequest request =
                ListCountersRequest.newBuilder()
                        .setSectorName("C")
                        .setCounterRange(CounterRange.newBuilder().setFrom(10).setTo(20).build())
                        .build();

        ListCountersResponse response = blockingStub.listCounters(request);

        Assertions.assertTrue(response.getCountersList().isEmpty());
    }
}
