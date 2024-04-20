package ar.edu.itba.pod.server.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.edu.itba.pod.grpc.query.*;
import ar.edu.itba.pod.server.models.*;
import ar.edu.itba.pod.server.repositories.CheckinRepository;
import ar.edu.itba.pod.server.repositories.CounterRepository;

import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;

import org.junit.Rule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class QueryServiceTest {

    @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();
    private final List<Checkin> checkins =
            List.of(
                    new Checkin("C", 2, "AmericanAirlines", "AA123", "ABC321"),
                    new Checkin("C", 3, "AmericanAirlines", "AA123", "XYZ999"),
                    new Checkin("A", 1, "AirCanada", "AC989", "XYZ123"));

    private final List<Sector> sectors =
            List.of(
                    new Sector(
                            "A",
                            List.of(
                                    new CountersRange(
                                            new Range(1, 1),
                                            new AssignedInfo("AirCanada", List.of("AC989"), 0)))),
                    new Sector(
                            "C",
                            List.of(
                                    new CountersRange(
                                            new Range(2, 3),
                                            new AssignedInfo(
                                                    "AmericanAirlines",
                                                    List.of("AA123", "AA124", "AA125"),
                                                    6)),
                                    new CountersRange(new Range(4, 4)),
                                    new CountersRange(new Range(7, 8)))),
                    new Sector("D", List.of(new CountersRange(new Range(5, 6)))));

    // Mock the repositories using mockito
    private final CheckinRepository checkinRepository = mock(CheckinRepository.class);
    private final CounterRepository counterRepository = mock(CounterRepository.class);
    private QueryServiceGrpc.QueryServiceBlockingStub blockingStub;

    @BeforeEach
    public void setUp() throws Exception {
        String serverName = InProcessServerBuilder.generateName();

        QueryService queryService = new QueryService(counterRepository, checkinRepository);

        grpcCleanup.register(
                InProcessServerBuilder.forName(serverName)
                        .directExecutor()
                        .addService(queryService)
                        .build()
                        .start());

        this.blockingStub =
                QueryServiceGrpc.newBlockingStub(
                        grpcCleanup.register(
                                InProcessChannelBuilder.forName(serverName)
                                        .directExecutor()
                                        .build()));
    }

    @Test
    public void testCheckinsNoCheckins() {
        when(checkinRepository.hasCheckins()).thenReturn(false);

        StatusRuntimeException exception =
                Assertions.assertThrows(
                        io.grpc.StatusRuntimeException.class,
                        () -> blockingStub.checkins(CheckinsRequest.newBuilder().build()));

        Assertions.assertEquals(
                io.grpc.Status.NOT_FOUND.getCode(), exception.getStatus().getCode());
        Assertions.assertEquals(
                "No checkins have been registered", exception.getStatus().getDescription());
    }

    @Test
    public void testCheckinsNoFilter() {
        when(checkinRepository.getCheckins(any()))
                .then(
                        invocation -> {
                            Predicate<Checkin> predicate = invocation.getArgument(0);
                            return checkins.stream().filter(predicate).toList();
                        });
        when(checkinRepository.hasCheckins()).thenReturn(true);

        // Test the service with no filters
        CheckinsResponse response = blockingStub.checkins(CheckinsRequest.newBuilder().build());

        // Assert that the response contains all the checkins in order
        for (int i = 0; i < checkins.size(); i++) {
            CheckinInfo checkinInfo = response.getCheckins(i);
            Checkin checkin = checkins.get(i);

            Assertions.assertEquals(checkin.sector(), checkinInfo.getSectorName());
            Assertions.assertEquals(checkin.counter(), checkinInfo.getCounter());
            Assertions.assertEquals(checkin.airline(), checkinInfo.getAirline());
            Assertions.assertEquals(checkin.flight(), checkinInfo.getFlight());
            Assertions.assertEquals(checkin.booking(), checkinInfo.getBooking());
        }
    }

    @Test
    public void testCheckinsFilterSector() {
        when(checkinRepository.getCheckins(any()))
                .then(
                        invocation -> {
                            Predicate<Checkin> predicate = invocation.getArgument(0);
                            return checkins.stream().filter(predicate).toList();
                        });
        when(checkinRepository.hasCheckins()).thenReturn(true);

        // Test the service with a sector filter
        CheckinsResponse response =
                blockingStub.checkins(CheckinsRequest.newBuilder().setSectorName("C").build());

        // Assert that the response contains only the checkins with sector C
        for (int i = 0; i < 2; i++) {
            CheckinInfo checkinInfo = response.getCheckins(i);
            Checkin checkin = checkins.get(i);

            Assertions.assertEquals(checkin.sector(), checkinInfo.getSectorName());
            Assertions.assertEquals(checkin.counter(), checkinInfo.getCounter());
            Assertions.assertEquals(checkin.airline(), checkinInfo.getAirline());
            Assertions.assertEquals(checkin.flight(), checkinInfo.getFlight());
            Assertions.assertEquals(checkin.booking(), checkinInfo.getBooking());
        }
    }

    @Test
    public void testCheckinsFilterAirline() {
        when(checkinRepository.getCheckins(any()))
                .then(
                        invocation -> {
                            Predicate<Checkin> predicate = invocation.getArgument(0);
                            return checkins.stream().filter(predicate).toList();
                        });
        when(checkinRepository.hasCheckins()).thenReturn(true);

        // Test the service with an airline filter
        CheckinsResponse response =
                blockingStub.checkins(CheckinsRequest.newBuilder().setAirline("AirCanada").build());

        // Assert that the response contains only the checkins with airline AmericanAirlines
        CheckinInfo checkinInfo = response.getCheckins(0);
        Checkin checkin = checkins.get(2);

        Assertions.assertEquals(checkin.sector(), checkinInfo.getSectorName());
        Assertions.assertEquals(checkin.counter(), checkinInfo.getCounter());
        Assertions.assertEquals(checkin.airline(), checkinInfo.getAirline());
        Assertions.assertEquals(checkin.flight(), checkinInfo.getFlight());
        Assertions.assertEquals(checkin.booking(), checkinInfo.getBooking());
    }

    @Test
    public void testCheckinsFilterSectorAndAirline() {
        when(checkinRepository.getCheckins(any()))
                .then(
                        invocation -> {
                            Predicate<Checkin> predicate = invocation.getArgument(0);
                            return checkins.stream().filter(predicate).toList();
                        });
        when(checkinRepository.hasCheckins()).thenReturn(true);

        // Test the service with a sector and airline filter
        CheckinsResponse response =
                blockingStub.checkins(
                        CheckinsRequest.newBuilder()
                                .setSectorName("C")
                                .setAirline("AmericanAirlines")
                                .build());

        // Assert that the response contains only the checkins with sector C and airline
        // AmericanAirlines
        for (int i = 0; i < 2; i++) {
            CheckinInfo checkinInfo = response.getCheckins(i);
            Checkin checkin = checkins.get(i);

            Assertions.assertEquals(checkin.sector(), checkinInfo.getSectorName());
            Assertions.assertEquals(checkin.counter(), checkinInfo.getCounter());
            Assertions.assertEquals(checkin.airline(), checkinInfo.getAirline());
            Assertions.assertEquals(checkin.flight(), checkinInfo.getFlight());
            Assertions.assertEquals(checkin.booking(), checkinInfo.getBooking());
        }
    }

    @Test
    public void testCheckinsFilterSectorAndAirlineEmpty() {
        when(checkinRepository.getCheckins(any()))
                .then(
                        invocation -> {
                            Predicate<Checkin> predicate = invocation.getArgument(0);
                            return checkins.stream().filter(predicate).toList();
                        });
        when(checkinRepository.hasCheckins()).thenReturn(true);

        // Test the service with a sector and airline filter
        CheckinsResponse response =
                blockingStub.checkins(
                        CheckinsRequest.newBuilder()
                                .setSectorName("C")
                                .setAirline("AirCanada")
                                .build());

        // Empty response
        Assertions.assertEquals(0, response.getCheckinsCount());
    }

    @Test
    public void testCountersNoCounters() {
        when(counterRepository.hasCounters()).thenReturn(false);

        StatusRuntimeException exception =
                Assertions.assertThrows(
                        io.grpc.StatusRuntimeException.class,
                        () -> blockingStub.counters(CountersRequest.newBuilder().build()));

        Assertions.assertEquals(
                io.grpc.Status.NOT_FOUND.getCode(), exception.getStatus().getCode());
        Assertions.assertEquals(
                "No counters have been added to this airport",
                exception.getStatus().getDescription());
    }

    @Test
    public void testCountersNoFilter() {
        when(counterRepository.getSectors()).thenReturn(sectors);
        when(counterRepository.hasCounters()).thenReturn(true);

        // Test the service with no filters
        CountersResponse response = blockingStub.counters(CountersRequest.newBuilder().build());

        // Assert that the response contains all the counters in order
        int counterIndex = 0;
        for (Sector sector : sectors) {
            for (int i = 0; i < sector.countersRangeList().size(); i++) {
                CountersRange countersRange = sector.countersRangeList().get(i);

                CountersInfo counterInfo = response.getCounters(counterIndex);

                Assertions.assertEquals(sector.sectorName(), counterInfo.getSectorName());
                Assertions.assertEquals(countersRange.range().from(), counterInfo.getCounters().getFrom());
                Assertions.assertEquals(countersRange.range().to(), counterInfo.getCounters().getTo());

                Optional<AssignedInfo> assignedInfo = countersRange.assignedInfo();
                if (assignedInfo.isPresent()) {
                    Assertions.assertEquals(assignedInfo.get().airline(), counterInfo.getAirline());

                    for (int j = 0; j < assignedInfo.get().flights().size(); j++) {
                        Assertions.assertEquals(
                                assignedInfo.get().flights().get(j), counterInfo.getFlights(j));
                    }

                    Assertions.assertEquals(assignedInfo.get().passengersInQueue(), counterInfo.getPassengersInQueue());
                }
                counterIndex++;
            }
        }
    }

    @Test
    public void testCountersFilterSector() {
        when(counterRepository.getSector("C")).thenReturn(Optional.of(sectors.get(1)));
        when(counterRepository.hasCounters()).thenReturn(true);

        // Test the service with a sector filter
        CountersResponse response =
                blockingStub.counters(CountersRequest.newBuilder().setSectorName("C").build());

        // Assert that the response contains only the counters with sector C
        Sector sector = sectors.get(1);
        for (int i = 0; i < sector.countersRangeList().size(); i++) {
            CountersRange countersRange = sector.countersRangeList().get(i);

            CountersInfo counterInfo = response.getCounters(i);

            Assertions.assertEquals(sector.sectorName(), counterInfo.getSectorName());
            Assertions.assertEquals(countersRange.range().from(), counterInfo.getCounters().getFrom());
            Assertions.assertEquals(countersRange.range().to(), counterInfo.getCounters().getTo());

            Optional<AssignedInfo> assignedInfo = countersRange.assignedInfo();
            if (assignedInfo.isPresent()) {
                Assertions.assertEquals(assignedInfo.get().airline(), counterInfo.getAirline());

                for (int j = 0; j < assignedInfo.get().flights().size(); j++) {
                    Assertions.assertEquals(
                            assignedInfo.get().flights().get(j), counterInfo.getFlights(j));
                }

                Assertions.assertEquals(assignedInfo.get().passengersInQueue(), counterInfo.getPassengersInQueue());
            }
        }
    }

    @Test
    public void testCountersFilterSectorEmpty() {
        when(counterRepository.getSector("Z")).thenReturn(Optional.empty());
        when(counterRepository.hasCounters()).thenReturn(true);

        // Test the service with a sector filter
        CountersResponse response =
                blockingStub.counters(CountersRequest.newBuilder().setSectorName("Z").build());

        // Empty response
        Assertions.assertEquals(0, response.getCountersCount());
    }
}
