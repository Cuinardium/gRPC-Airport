package ar.edu.itba.pod.server.services;

import static org.mockito.Mockito.*;

import ar.edu.itba.pod.grpc.common.CounterRange;
import ar.edu.itba.pod.grpc.events.EventType;
import ar.edu.itba.pod.grpc.events.PassengerArrivedInfo;
import ar.edu.itba.pod.grpc.events.RegisterResponse;
import ar.edu.itba.pod.grpc.passenger.*;
import ar.edu.itba.pod.server.events.EventManager;
import ar.edu.itba.pod.server.models.*;
import ar.edu.itba.pod.server.repositories.CheckinRepository;
import ar.edu.itba.pod.server.repositories.CounterRepository;
import ar.edu.itba.pod.server.repositories.PassengerRepository;
import ar.edu.itba.pod.server.utils.Pair;

import io.grpc.Status;
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

public class PassengerServiceTest {

    @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    private final CounterRepository counterRepository = mock(CounterRepository.class);
    private final PassengerRepository passengerRepository = mock(PassengerRepository.class);
    private final CheckinRepository checkinRepository = mock(CheckinRepository.class);
    private final EventManager eventManager = mock(EventManager.class);

    private final CountersRange countersRange =
            new CountersRange(
                    new Range(3, 4), new AssignedInfo("AmericanAirlines", List.of("AA123"), 7));
    private final Passenger passenger = new Passenger("XYZ345", "AA123", "AmericanAirlines");
    private final String sector = "A";
    private final Checkin checkin = new Checkin("A", 4, "AmericanAirlines", "AA123", "XYZ345");

    private PassengerServiceGrpc.PassengerServiceBlockingStub blockingStub;

    @BeforeEach
    public void setUp() throws Exception {
        String serverName = InProcessServerBuilder.generateName();

        PassengerService passengerService =
                new PassengerService(
                        counterRepository, passengerRepository, checkinRepository, eventManager);

        grpcCleanup.register(
                InProcessServerBuilder.forName(serverName)
                        .directExecutor()
                        .addService(passengerService)
                        .build()
                        .start());

        blockingStub =
                PassengerServiceGrpc.newBlockingStub(
                        grpcCleanup.register(
                                InProcessChannelBuilder.forName(serverName)
                                        .directExecutor()
                                        .build()));
    }

    @Test
    public void testFetchCounterNoBooking() {

        StatusRuntimeException exception =
                Assertions.assertThrows(
                        StatusRuntimeException.class,
                        () -> blockingStub.fetchCounter(FetchCounterRequest.newBuilder().build()));

        Assertions.assertEquals(Status.INVALID_ARGUMENT.getCode(), exception.getStatus().getCode());
        Assertions.assertEquals("No booking was provided", exception.getStatus().getDescription());
    }

    @Test
    public void testFetchCounterExpectedPassengerNotFound() {

        when(passengerRepository.getPassenger("123")).thenReturn(Optional.empty());

        StatusRuntimeException exception =
                Assertions.assertThrows(
                        StatusRuntimeException.class,
                        () ->
                                blockingStub.fetchCounter(
                                        FetchCounterRequest.newBuilder()
                                                .setBooking("123")
                                                .build()));

        Assertions.assertEquals(Status.NOT_FOUND.getCode(), exception.getStatus().getCode());
        Assertions.assertEquals(
                "No passenger expected with the given booking",
                exception.getStatus().getDescription());
    }

    @Test
    public void testFetchCounterFlightAssigned() {
        when(passengerRepository.getPassenger("XYZ345")).thenReturn(Optional.of(passenger));
        when(counterRepository.getFlightCountersAndSector("AA123"))
                .thenReturn(Optional.of(new Pair<>(countersRange, sector)));

        FetchCounterResponse response =
                blockingStub.fetchCounter(
                        FetchCounterRequest.newBuilder().setBooking("XYZ345").build());

        Assertions.assertEquals(FlightStatus.FLIGHT_STATUS_ASSIGNED, response.getStatus());

        Assertions.assertEquals(sector, response.getSector());

        Assertions.assertEquals(passenger.airline(), response.getAirline());
        Assertions.assertEquals(passenger.flight(), response.getFlight());
        Assertions.assertEquals(
                countersRange.assignedInfo().get().passengersInQueue(),
                response.getPassengersInQueue());

        Assertions.assertEquals(countersRange.range().from(), response.getCounters().getFrom());
        Assertions.assertEquals(countersRange.range().to(), response.getCounters().getTo());
    }

    @Test
    public void testFetchCounterFlightNotAssigned() {
        when(passengerRepository.getPassenger("XYZ345")).thenReturn(Optional.of(passenger));
        when(counterRepository.getFlightCountersAndSector("AA123")).thenReturn(Optional.empty());

        FetchCounterResponse response =
                blockingStub.fetchCounter(
                        FetchCounterRequest.newBuilder().setBooking("XYZ345").build());

        Assertions.assertEquals(FlightStatus.FLIGHT_STATUS_UNASSIGNED, response.getStatus());

        Assertions.assertEquals(passenger.airline(), response.getAirline());
        Assertions.assertEquals(passenger.flight(), response.getFlight());
    }

    @Test
    public void testPassengerCheckinNoSector() {
        StatusRuntimeException exception =
                Assertions.assertThrows(
                        StatusRuntimeException.class,
                        () ->
                                blockingStub.passengerCheckin(
                                        PassengerCheckinRequest.newBuilder()
                                                .setBooking("XYZ345")
                                                .setCounter(3)
                                                .build()));

        Assertions.assertEquals(Status.INVALID_ARGUMENT.getCode(), exception.getStatus().getCode());
        Assertions.assertEquals(
                "Sector name, booking and counter must be provided",
                exception.getStatus().getDescription());
    }

    @Test
    public void testPassengerCheckinNoBooking() {
        StatusRuntimeException exception =
                Assertions.assertThrows(
                        StatusRuntimeException.class,
                        () ->
                                blockingStub.passengerCheckin(
                                        PassengerCheckinRequest.newBuilder()
                                                .setSectorName("A")
                                                .setCounter(3)
                                                .build()));

        Assertions.assertEquals(Status.INVALID_ARGUMENT.getCode(), exception.getStatus().getCode());
        Assertions.assertEquals(
                "Sector name, booking and counter must be provided",
                exception.getStatus().getDescription());
    }

    @Test
    public void testPassengerCheckinNoCounter() {
        StatusRuntimeException exception =
                Assertions.assertThrows(
                        StatusRuntimeException.class,
                        () ->
                                blockingStub.passengerCheckin(
                                        PassengerCheckinRequest.newBuilder()
                                                .setSectorName("A")
                                                .setBooking("XYZ345")
                                                .build()));

        Assertions.assertEquals(Status.INVALID_ARGUMENT.getCode(), exception.getStatus().getCode());
        Assertions.assertEquals(
                "Sector name, booking and counter must be provided",
                exception.getStatus().getDescription());
    }

    @Test
    public void testPassengerCheckinExpectedPassengerNotFound() {
        when(passengerRepository.getPassenger("XYZ345")).thenReturn(Optional.empty());

        StatusRuntimeException exception =
                Assertions.assertThrows(
                        StatusRuntimeException.class,
                        () ->
                                blockingStub.passengerCheckin(
                                        PassengerCheckinRequest.newBuilder()
                                                .setSectorName("A")
                                                .setBooking("XYZ345")
                                                .setCounter(3)
                                                .build()));

        Assertions.assertEquals(Status.NOT_FOUND.getCode(), exception.getStatus().getCode());
        Assertions.assertEquals(
                "No passenger expected with the given booking",
                exception.getStatus().getDescription());
    }

    @Test
    public void testPassengerCheckinSectorNotFound() {
        when(passengerRepository.getPassenger("XYZ345")).thenReturn(Optional.of(passenger));
        when(counterRepository.hasSector("A")).thenReturn(false);

        StatusRuntimeException exception =
                Assertions.assertThrows(
                        StatusRuntimeException.class,
                        () ->
                                blockingStub.passengerCheckin(
                                        PassengerCheckinRequest.newBuilder()
                                                .setSectorName("A")
                                                .setBooking("XYZ345")
                                                .setCounter(3)
                                                .build()));

        Assertions.assertEquals(Status.NOT_FOUND.getCode(), exception.getStatus().getCode());
        Assertions.assertEquals(
                "Provided sector does not exist", exception.getStatus().getDescription());
    }

    @Test
    public void testPassengerCheckinCounterNotFound() {
        when(passengerRepository.getPassenger("XYZ345")).thenReturn(Optional.of(passenger));
        when(counterRepository.hasSector("A")).thenReturn(true);
        when(counterRepository.getFlightCounters("AA123")).thenReturn(Optional.empty());

        StatusRuntimeException exception =
                Assertions.assertThrows(
                        StatusRuntimeException.class,
                        () ->
                                blockingStub.passengerCheckin(
                                        PassengerCheckinRequest.newBuilder()
                                                .setSectorName("A")
                                                .setBooking("XYZ345")
                                                .setCounter(3)
                                                .build()));

        Assertions.assertEquals(Status.NOT_FOUND.getCode(), exception.getStatus().getCode());
        Assertions.assertEquals(
                "The indicated does not exist or it is not accepting checkins for the booking flight",
                exception.getStatus().getDescription());
    }

    @Test
    public void testPassengerCheckinCounterInvalid() {
        when(passengerRepository.getPassenger("XYZ345")).thenReturn(Optional.of(passenger));
        when(counterRepository.hasSector("A")).thenReturn(true);
        when(counterRepository.getFlightCounters("AA123")).thenReturn(Optional.of(countersRange));

        StatusRuntimeException exception =
                Assertions.assertThrows(
                        StatusRuntimeException.class,
                        () ->
                                blockingStub.passengerCheckin(
                                        PassengerCheckinRequest.newBuilder()
                                                .setSectorName("A")
                                                .setBooking("XYZ345")
                                                .setCounter(4)
                                                .build()));

        Assertions.assertEquals(Status.NOT_FOUND.getCode(), exception.getStatus().getCode());
        Assertions.assertEquals(
                "The indicated does not exist or it is not accepting checkins for the booking flight",
                exception.getStatus().getDescription());
    }

    @Test
    public void testPassengerCheckinAlreadyInQueue() {
        when(passengerRepository.getPassenger("XYZ345")).thenReturn(Optional.of(passenger));
        when(counterRepository.hasSector("A")).thenReturn(true);
        when(counterRepository.getFlightCounters("AA123")).thenReturn(Optional.of(countersRange));
        when(counterRepository.hasPassengerInCounter(countersRange.range(), "XYZ345"))
                .thenReturn(true);

        StatusRuntimeException exception =
                Assertions.assertThrows(
                        StatusRuntimeException.class,
                        () ->
                                blockingStub.passengerCheckin(
                                        PassengerCheckinRequest.newBuilder()
                                                .setSectorName("A")
                                                .setBooking("XYZ345")
                                                .setCounter(3)
                                                .build()));

        Assertions.assertEquals(Status.ALREADY_EXISTS.getCode(), exception.getStatus().getCode());
        Assertions.assertEquals(
                "Passenger is already waiting in counter queue",
                exception.getStatus().getDescription());
    }

    @Test
    public void testPassengerAlreadyCheckedIn() {
        when(passengerRepository.getPassenger("XYZ345")).thenReturn(Optional.of(passenger));
        when(counterRepository.hasSector("A")).thenReturn(true);
        when(counterRepository.getFlightCounters("AA123")).thenReturn(Optional.of(countersRange));
        when(counterRepository.hasPassengerInCounter(countersRange.range(), "XYZ345"))
                .thenReturn(false);
        when(checkinRepository.hasCheckin("XYZ345")).thenReturn(true);

        StatusRuntimeException exception =
                Assertions.assertThrows(
                        StatusRuntimeException.class,
                        () ->
                                blockingStub.passengerCheckin(
                                        PassengerCheckinRequest.newBuilder()
                                                .setSectorName("A")
                                                .setBooking("XYZ345")
                                                .setCounter(3)
                                                .build()));

        Assertions.assertEquals(Status.ALREADY_EXISTS.getCode(), exception.getStatus().getCode());
        Assertions.assertEquals(
                "Passenger has already completed checkin", exception.getStatus().getDescription());
    }

    @Test
    public void testPassengerCheckinSuccess() {
        when(passengerRepository.getPassenger("XYZ345")).thenReturn(Optional.of(passenger));
        when(counterRepository.hasSector("A")).thenReturn(true);
        when(counterRepository.getFlightCounters("AA123")).thenReturn(Optional.of(countersRange));
        when(counterRepository.hasPassengerInCounter(countersRange.range(), "XYZ345"))
                .thenReturn(false);
        when(checkinRepository.hasCheckin("XYZ345")).thenReturn(false);
        when(counterRepository.addPassengerToQueue(countersRange.range(), "XYZ345"))
                .thenReturn(countersRange.assignedInfo().get().passengersInQueue());
        when(eventManager.notify(any(), any())).thenReturn(true);

        PassengerCheckinResponse response =
                blockingStub.passengerCheckin(
                        PassengerCheckinRequest.newBuilder()
                                .setSectorName("A")
                                .setBooking("XYZ345")
                                .setCounter(3)
                                .build());

        Assertions.assertEquals(
                countersRange.assignedInfo().get().flights().get(0), response.getFlight());
        Assertions.assertEquals(
                countersRange.assignedInfo().get().airline(), response.getAirline());
        Assertions.assertEquals(countersRange.range().from(), response.getCounters().getFrom());
        Assertions.assertEquals(countersRange.range().to(), response.getCounters().getTo());
        Assertions.assertEquals(
                countersRange.assignedInfo().get().passengersInQueue(),
                response.getPassengersInQueue());
        Assertions.assertEquals(sector, response.getSector());

        verify(eventManager, times(1))
                .notify(
                        passenger.airline(),
                        RegisterResponse.newBuilder()
                                .setEventType(EventType.EVENT_TYPE_PASSENGER_ARRIVED)
                                .setPassengerArrivedInfo(
                                        PassengerArrivedInfo.newBuilder()
                                                .setBooking("XYZ345")
                                                .setFlight("AA123")
                                                .setSectorName("A")
                                                .setCounters(
                                                        CounterRange.newBuilder()
                                                                .setFrom(
                                                                        countersRange
                                                                                .range()
                                                                                .from())
                                                                .setTo(countersRange.range().to())
                                                                .build())
                                                .setPassengersInQueue(
                                                        countersRange
                                                                .assignedInfo()
                                                                .get()
                                                                .passengersInQueue())
                                                .build())
                                .build());
    }

    @Test
    public void testPassengerStatusNoBooking() {
        StatusRuntimeException exception =
                Assertions.assertThrows(
                        StatusRuntimeException.class,
                        () ->
                                blockingStub.passengerStatus(
                                        PassengerStatusRequest.newBuilder().build()));

        Assertions.assertEquals(Status.INVALID_ARGUMENT.getCode(), exception.getStatus().getCode());
        Assertions.assertEquals("No booking was provided", exception.getStatus().getDescription());
    }

    @Test
    public void testPassengerStatusExpectedPassengerNotFound() {
        when(passengerRepository.getPassenger("123")).thenReturn(Optional.empty());

        StatusRuntimeException exception =
                Assertions.assertThrows(
                        StatusRuntimeException.class,
                        () ->
                                blockingStub.passengerStatus(
                                        PassengerStatusRequest.newBuilder()
                                                .setBooking("123")
                                                .build()));

        Assertions.assertEquals(Status.NOT_FOUND.getCode(), exception.getStatus().getCode());
        Assertions.assertEquals(
                "No passenger expected with the given booking",
                exception.getStatus().getDescription());
    }

    @Test
    public void testPassengerStatusFlightUnassigned() {
        when(passengerRepository.getPassenger("XYZ345")).thenReturn(Optional.of(passenger));
        when(counterRepository.getFlightCountersAndSector("AA123")).thenReturn(Optional.empty());

        StatusRuntimeException exception =
                Assertions.assertThrows(
                        StatusRuntimeException.class,
                        () ->
                                blockingStub.passengerStatus(
                                        PassengerStatusRequest.newBuilder()
                                                .setBooking("XYZ345")
                                                .build()));

        Assertions.assertEquals(Status.NOT_FOUND.getCode(), exception.getStatus().getCode());
        Assertions.assertEquals(
                "No counters were assigned to the bookings flight",
                exception.getStatus().getDescription());
    }

    @Test
    public void testPassengerStatusCheckedIn() {
        when(passengerRepository.getPassenger("XYZ345")).thenReturn(Optional.of(passenger));
        when(counterRepository.getFlightCountersAndSector("AA123"))
                .thenReturn(Optional.of(new Pair<>(countersRange, sector)));
        when(checkinRepository.getCheckin("XYZ345")).thenReturn(Optional.of(checkin));

        PassengerStatusResponse response =
                blockingStub.passengerStatus(
                        PassengerStatusRequest.newBuilder().setBooking("XYZ345").build());

        Assertions.assertEquals(PassengerStatus.PASSENGER_STATUS_CHECKED_IN, response.getStatus());
        Assertions.assertEquals(checkin.flight(), response.getFlight());
        Assertions.assertEquals(checkin.airline(), response.getAirline());
        Assertions.assertEquals(sector, response.getSectorName());
        Assertions.assertEquals(checkin.counter(), response.getCheckedInCounter());
    }

    @Test
    public void testPassengerStatusWaiting() {
        when(passengerRepository.getPassenger("XYZ345")).thenReturn(Optional.of(passenger));
        when(counterRepository.getFlightCountersAndSector("AA123"))
                .thenReturn(Optional.of(new Pair<>(countersRange, sector)));
        when(checkinRepository.getCheckin("XYZ345")).thenReturn(Optional.empty());
        when(counterRepository.hasPassengerInCounter(countersRange.range(), "XYZ345"))
                .thenReturn(true);

        PassengerStatusResponse response =
                blockingStub.passengerStatus(
                        PassengerStatusRequest.newBuilder().setBooking("XYZ345").build());

        Assertions.assertEquals(PassengerStatus.PASSENGER_STATUS_WAITING, response.getStatus());
        Assertions.assertEquals(
                countersRange.assignedInfo().get().flights().get(0), response.getFlight());
        Assertions.assertEquals(
                countersRange.assignedInfo().get().airline(), response.getAirline());
        Assertions.assertEquals(sector, response.getSectorName());
        Assertions.assertEquals(countersRange.range().from(), response.getCounters().getFrom());
        Assertions.assertEquals(countersRange.range().to(), response.getCounters().getTo());
        Assertions.assertEquals(
                countersRange.assignedInfo().get().passengersInQueue(),
                response.getPassengersInQueue());
    }

    @Test
    public void testPassengerStatusNotArrived() {
        when(passengerRepository.getPassenger("XYZ345")).thenReturn(Optional.of(passenger));
        when(counterRepository.getFlightCountersAndSector("AA123"))
                .thenReturn(Optional.of(new Pair<>(countersRange, sector)));
        when(checkinRepository.getCheckin("XYZ345")).thenReturn(Optional.empty());
        when(counterRepository.hasPassengerInCounter(countersRange.range(), "XYZ345"))
                .thenReturn(false);

        PassengerStatusResponse response =
                blockingStub.passengerStatus(
                        PassengerStatusRequest.newBuilder().setBooking("XYZ345").build());

        Assertions.assertEquals(PassengerStatus.PASSENGER_STATUS_NOT_ARRIVED, response.getStatus());
        Assertions.assertEquals(
                countersRange.assignedInfo().get().flights().get(0), response.getFlight());
        Assertions.assertEquals(
                countersRange.assignedInfo().get().airline(), response.getAirline());
        Assertions.assertEquals(sector, response.getSectorName());
        Assertions.assertEquals(countersRange.range().from(), response.getCounters().getFrom());
        Assertions.assertEquals(countersRange.range().to(), response.getCounters().getTo());
    }
}
