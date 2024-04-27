package ar.edu.itba.pod.server.events;

import ar.edu.itba.pod.grpc.common.CounterRange;
import ar.edu.itba.pod.grpc.events.*;
import ar.edu.itba.pod.server.exceptions.AlreadyExistsException;

import io.grpc.stub.StreamObserver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class EventManagerTest {

    private final List<RegisterResponse> events =
            List.of(
                    RegisterResponse.newBuilder()
                            .setEventType(EventType.EVENT_TYPE_AIRLINE_REGISTERED)
                            .build(),
                    RegisterResponse.newBuilder()
                            .setEventType(EventType.EVENT_TYPE_COUNTERS_ASSIGNED)
                            .setCountersAssignedInfo(
                                    CountersAssignedInfo.newBuilder()
                                            .setSectorName("C")
                                            .setCounters(
                                                    CounterRange.newBuilder()
                                                            .setFrom(3)
                                                            .setTo(4)
                                                            .build())
                                            .addAllFlights(List.of("AA123", "AA124", "AA125"))
                                            .build())
                            .build(),
                    RegisterResponse.newBuilder()
                            .setEventType(EventType.EVENT_TYPE_COUNTERS_FREED)
                            .setCountersFreedInfo(
                                    CountersFreedInfo.newBuilder()
                                            .setSectorName("C")
                                            .setCounters(
                                                    CounterRange.newBuilder()
                                                            .setFrom(2)
                                                            .setTo(4)
                                                            .build())
                                            .addAllFlights(List.of("AA123", "AA124", "AA125"))
                                            .build())
                            .build(),
                    RegisterResponse.newBuilder()
                            .setEventType(EventType.EVENT_TYPE_PASSENGER_ARRIVED)
                            .setPassengerArrivedInfo(
                                    PassengerArrivedInfo.newBuilder()
                                            .setBooking("ABC123")
                                            .setFlight("AA123")
                                            .setSectorName("C")
                                            .setCounters(
                                                    CounterRange.newBuilder()
                                                            .setFrom(2)
                                                            .setTo(4)
                                                            .build())
                                            .setPassengersInQueue(6)
                                            .build())
                            .build(),
                    RegisterResponse.newBuilder()
                            .setEventType(EventType.EVENT_TYPE_PASSENGER_CHECKED_IN)
                            .setPassengerCheckedInInfo(
                                    PassengerCheckedInInfo.newBuilder()
                                            .setBooking("XYZ345")
                                            .setFlight("AA123")
                                            .setSectorName("C")
                                            .setCounter(3)
                                            .build())
                            .build(),
                    RegisterResponse.newBuilder()
                            .setEventType(EventType.EVENT_TYPE_ASSIGNATION_PENDING)
                            .setAssignationPendingInfo(
                                    AssignationPendingInfo.newBuilder()
                                            .setSectorName("C")
                                            .addAllFlights(List.of("AA888", "AA999"))
                                            .setCounterCount(7)
                                            .setPendingAssignations(5)
                                            .build())
                            .build(),
                    RegisterResponse.newBuilder()
                            .setEventType(EventType.EVENT_TYPE_MOVED_IN_ASSIGNATION_QUEUE)
                            .setMovedInAssignationQueueInfo(
                                    MovedInAssignationQueueInfo.newBuilder()
                                            .setSectorName("C")
                                            .addAllFlights(List.of("AA888", "AA999"))
                                            .setCounterCount(7)
                                            .setPendingAssignations(4)
                                            .build())
                            .build());

    private EventManager eventManager;

    @BeforeEach
    public final void setUp() {
        this.eventManager = new EventManagerImpl();
    }

    @Test
    public void testRegisterNotify() throws AlreadyExistsException {
        String airline = "AmericanAirlines";
        List<RegisterResponse> events = new ArrayList<>();
        StreamObserver<RegisterResponse> eventStream =
                new StreamObserver<>() {
                    @Override
                    public void onNext(RegisterResponse value) {
                        events.add(value);
                    }

                    @Override
                    public void onError(Throwable t) {}

                    @Override
                    public void onCompleted() {}
                };

        eventManager.register(airline, eventStream);

        for (RegisterResponse event : this.events) {
            Assertions.assertTrue(eventManager.notify(airline, event));
        }

        Assertions.assertEquals(this.events, events);
    }

    @Test
    public void testRegisterAlreadyExists() throws AlreadyExistsException {
        String airline = "AmericanAirlines";
        StreamObserver<RegisterResponse> eventStream =
                new StreamObserver<>() {
                    @Override
                    public void onNext(RegisterResponse value) {}

                    @Override
                    public void onError(Throwable t) {}

                    @Override
                    public void onCompleted() {}
                };

        eventManager.register(airline, eventStream);

        Assertions.assertThrows(
                AlreadyExistsException.class, () -> eventManager.register(airline, eventStream));
    }

    @Test
    public void testUnregister() throws AlreadyExistsException, NoSuchElementException {
        String airline = "AmericanAirlines";


        StreamObserver<RegisterResponse> eventStream =
                new StreamObserver<>() {

                    private boolean completed = false;

                    @Override
                    public void onNext(RegisterResponse value) {
                        if (completed) {
                            throw new IllegalStateException("Stream is closed");
                        }
                    }

                    @Override
                    public void onError(Throwable t) {}

                    @Override
                    public void onCompleted() {
                        completed = true;
                    }
                };

        eventManager.register(airline, eventStream);
        eventManager.unregister(airline);

        Assertions.assertThrows(NoSuchElementException.class, () -> eventManager.unregister(airline));

        // Assert that the stream was closed
        Assertions.assertThrows(
                IllegalStateException.class, () -> eventStream.onNext(RegisterResponse.getDefaultInstance()));
    }

    @Test
    public void testUnregisterNotRegistered() {
        String airline = "AmericanAirlines";

        Assertions.assertThrows(NoSuchElementException.class, () -> eventManager.unregister(airline));
    }

    @Test
    public void testNotifyNotRegistered() {
        String airline = "AmericanAirlines";

        for (RegisterResponse event : this.events) {
            Assertions.assertFalse(eventManager.notify(airline, event));
        }
    }
}
