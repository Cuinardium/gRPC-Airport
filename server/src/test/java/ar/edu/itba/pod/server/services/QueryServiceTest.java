package ar.edu.itba.pod.server.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.edu.itba.pod.grpc.query.*;
import ar.edu.itba.pod.server.models.Checkin;
import ar.edu.itba.pod.server.models.CountersRange;
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
import java.util.function.Predicate;

public class QueryServiceTest {

    @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();
    private final List<Checkin> checkins =
            List.of(
                    new Checkin("C", 2, "AmericanAirlines", "AA123", "ABC321"),
                    new Checkin("C", 3, "AmericanAirlines", "AA123", "XYZ999"),
                    new Checkin("A", 1, "AirCanada", "AC989", "XYZ123"));

    private final List<CountersRange> counters =

    // Mock the repositories using mockito
    private final CheckinRepository checkinRepository = mock(CheckinRepository.class);
    private final CounterRepository counterRepository = mock(CounterRepository.class);
    private String serverName;
    private QueryServiceGrpc.QueryServiceBlockingStub blockingStub;

    @BeforeEach
    public void setUp() throws Exception {
        this.serverName = InProcessServerBuilder.generateName();

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
                        CheckinsRequest.newBuilder().setSectorName("C").setAirline("AmericanAirlines").build());

        // Assert that the response contains only the checkins with sector C and airline AmericanAirlines
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
                        CheckinsRequest.newBuilder().setSectorName("C").setAirline("AirCanada").build());

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
                "No counters have been added to this airport", exception.getStatus().getDescription());
    }

    @Test

}
