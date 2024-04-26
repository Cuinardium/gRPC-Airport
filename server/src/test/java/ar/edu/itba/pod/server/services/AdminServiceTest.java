package ar.edu.itba.pod.server.services;

import static org.mockito.Mockito.*;

import ar.edu.itba.pod.grpc.admin.AddCountersRequest;
import ar.edu.itba.pod.grpc.admin.AddPassengerRequest;
import ar.edu.itba.pod.grpc.admin.AddSectorRequest;
import ar.edu.itba.pod.grpc.admin.AdminServiceGrpc;
import ar.edu.itba.pod.grpc.common.CounterRange;
import ar.edu.itba.pod.server.exceptions.AlreadyExistsException;
import ar.edu.itba.pod.server.exceptions.NotFoundException;
import ar.edu.itba.pod.server.models.Passenger;
import ar.edu.itba.pod.server.models.Range;
import ar.edu.itba.pod.server.repositories.CounterRepository;
import ar.edu.itba.pod.server.repositories.PassengerRepository;

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

@RunWith(JUnit4.class)
public class AdminServiceTest {

    @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    private final String sector = "A";
    private final int counterCount = 5;
    private final Passenger passenger = new Passenger("123456", "AR1234", "Aerolineas Argentinas");

    private final CounterRepository counterRepository = mock(CounterRepository.class);
    private final PassengerRepository passengerRepository = mock(PassengerRepository.class);

    private AdminServiceGrpc.AdminServiceBlockingStub blockingStub;

    @Before
    public void setUp() throws Exception {
        String serverName = InProcessServerBuilder.generateName();

        AdminService adminService = new AdminService(counterRepository, passengerRepository);

        grpcCleanup.register(
                InProcessServerBuilder.forName(serverName)
                        .directExecutor()
                        .addService(adminService)
                        .build()
                        .start());

        blockingStub =
                AdminServiceGrpc.newBlockingStub(
                        grpcCleanup.register(
                                InProcessChannelBuilder.forName(serverName)
                                        .directExecutor()
                                        .build()));
    }

    @Test
    public void testAddSectorNoSector() {
        StatusRuntimeException exception =
                Assertions.assertThrows(
                        StatusRuntimeException.class,
                        () -> blockingStub.addSector(AddSectorRequest.newBuilder().build()));

        Assertions.assertEquals(Status.INVALID_ARGUMENT.getCode(), exception.getStatus().getCode());
        Assertions.assertEquals("No sector was specified", exception.getStatus().getDescription());
    }

    @Test
    public void testAddSectorAlreadyExists() throws AlreadyExistsException {
        doThrow(AlreadyExistsException.class).when(counterRepository).addSector(sector);

        StatusRuntimeException exception =
                Assertions.assertThrows(
                        StatusRuntimeException.class,
                        () ->
                                blockingStub.addSector(
                                        AddSectorRequest.newBuilder()
                                                .setSectorName(sector)
                                                .build()));

        Assertions.assertEquals(Status.ALREADY_EXISTS.getCode(), exception.getStatus().getCode());
        Assertions.assertEquals(
                "This sector was already added", exception.getStatus().getDescription());
    }

    @Test
    public void testAddSector() throws AlreadyExistsException {
        doNothing().when(counterRepository).addSector(sector);

        Assertions.assertDoesNotThrow(
                () ->
                        blockingStub.addSector(
                                AddSectorRequest.newBuilder().setSectorName(sector).build()));
    }

    @Test
    public void testAddCountersNoSector() {
        StatusRuntimeException exception =
                Assertions.assertThrows(
                        StatusRuntimeException.class,
                        () ->
                                blockingStub.addCounters(
                                        AddCountersRequest.newBuilder()
                                                .setCounterCount(counterCount)
                                                .build()));

        Assertions.assertEquals(Status.INVALID_ARGUMENT.getCode(), exception.getStatus().getCode());
        Assertions.assertEquals("No sector was specified", exception.getStatus().getDescription());
    }

    @Test
    public void testAddCountersInvalidCounterCount() {
        StatusRuntimeException exception =
                Assertions.assertThrows(
                        StatusRuntimeException.class,
                        () ->
                                blockingStub.addCounters(
                                        AddCountersRequest.newBuilder()
                                                .setSectorName(sector)
                                                .build()));

        Assertions.assertEquals(Status.INVALID_ARGUMENT.getCode(), exception.getStatus().getCode());
        Assertions.assertEquals(
                "The amount of counters needs to be a positive number",
                exception.getStatus().getDescription());
    }

    @Test
    public void testAddCountersSectorNotFound() throws NotFoundException {
        doThrow(NotFoundException.class).when(counterRepository).addCounters(sector, counterCount);

        StatusRuntimeException exception =
                Assertions.assertThrows(
                        StatusRuntimeException.class,
                        () ->
                                blockingStub.addCounters(
                                        AddCountersRequest.newBuilder()
                                                .setSectorName(sector)
                                                .setCounterCount(counterCount)
                                                .build()));

        Assertions.assertEquals(Status.NOT_FOUND.getCode(), exception.getStatus().getCode());
        Assertions.assertEquals(
                "The specified sector does not exist", exception.getStatus().getDescription());
    }

    @Test
    public void testAddCountersSuccess() throws NotFoundException {
        when(counterRepository.addCounters(sector, counterCount))
                .thenReturn(new Range(1, counterCount));

        CounterRange response =
                blockingStub.addCounters(
                        AddCountersRequest.newBuilder()
                                .setSectorName(sector)
                                .setCounterCount(counterCount)
                                .build());

        Assertions.assertEquals(1, response.getFrom());
        Assertions.assertEquals(counterCount, response.getTo());
    }

    @Test
    public void testAddPassengerNoBooking() {
        StatusRuntimeException exception =
                Assertions.assertThrows(
                        StatusRuntimeException.class,
                        () ->
                                blockingStub.addPassenger(
                                        AddPassengerRequest.newBuilder()
                                                .setAirline(passenger.airline())
                                                .setFlight(passenger.flight())
                                                .build()));

        Assertions.assertEquals(Status.INVALID_ARGUMENT.getCode(), exception.getStatus().getCode());
        Assertions.assertEquals(
                "Booking, airline and flight must be provided.",
                exception.getStatus().getDescription());
    }

    @Test
    public void testAddPassengerNoAirline() {
        StatusRuntimeException exception =
                Assertions.assertThrows(
                        StatusRuntimeException.class,
                        () ->
                                blockingStub.addPassenger(
                                        AddPassengerRequest.newBuilder()
                                                .setBooking(passenger.booking())
                                                .setFlight(passenger.flight())
                                                .build()));

        Assertions.assertEquals(Status.INVALID_ARGUMENT.getCode(), exception.getStatus().getCode());
        Assertions.assertEquals(
                "Booking, airline and flight must be provided.",
                exception.getStatus().getDescription());
    }

    @Test
    public void testAddPassengerNoFlight() {
        StatusRuntimeException exception =
                Assertions.assertThrows(
                        StatusRuntimeException.class,
                        () ->
                                blockingStub.addPassenger(
                                        AddPassengerRequest.newBuilder()
                                                .setBooking(passenger.booking())
                                                .setAirline(passenger.airline())
                                                .build()));

        Assertions.assertEquals(Status.INVALID_ARGUMENT.getCode(), exception.getStatus().getCode());
        Assertions.assertEquals(
                "Booking, airline and flight must be provided.",
                exception.getStatus().getDescription());
    }

    @Test
    public void testAddPassengerAlreadyExists() throws AlreadyExistsException {
        doThrow(AlreadyExistsException.class).when(passengerRepository).addPassenger(passenger);

        StatusRuntimeException exception =
                Assertions.assertThrows(
                        StatusRuntimeException.class,
                        () ->
                                blockingStub.addPassenger(
                                        AddPassengerRequest.newBuilder()
                                                .setBooking(passenger.booking())
                                                .setAirline(passenger.airline())
                                                .setFlight(passenger.flight())
                                                .build()));

        Assertions.assertEquals(Status.ALREADY_EXISTS.getCode(), exception.getStatus().getCode());
        Assertions.assertEquals(
                "This passenger was already added", exception.getStatus().getDescription());
    }

    @Test
    public void testAddPassengerSuccess() throws AlreadyExistsException {
        doNothing().when(passengerRepository).addPassenger(passenger);

        Assertions.assertDoesNotThrow(
                () ->
                        blockingStub.addPassenger(
                                AddPassengerRequest.newBuilder()
                                        .setBooking(passenger.booking())
                                        .setAirline(passenger.airline())
                                        .setFlight(passenger.flight())
                                        .build()));

        verify(passengerRepository).addPassenger(passenger);
    }
}
