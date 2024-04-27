package ar.edu.itba.pod.server.services;

import static org.mockito.Mockito.*;

import ar.edu.itba.pod.grpc.events.*;
import ar.edu.itba.pod.server.events.EventManager;
import ar.edu.itba.pod.server.exceptions.AlreadyExistsException;
import ar.edu.itba.pod.server.repositories.PassengerRepository;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RunWith(JUnit4.class)
public class EventServiceTest {

    @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    private final EventManager eventManager = mock(EventManager.class);
    private final PassengerRepository passengerRepository = mock(PassengerRepository.class);

    private EventsServiceGrpc.EventsServiceBlockingStub blockingStub;
    private EventsServiceGrpc.EventsServiceStub asyncStub;
    private ManagedChannel channel;

    @Before
    public final void setUp() throws Exception {
        String serverName = InProcessServerBuilder.generateName();

        EventsService eventsService = new EventsService(passengerRepository, eventManager);

        grpcCleanup.register(
                InProcessServerBuilder.forName(serverName)
                        .directExecutor()
                        .addService(eventsService)
                        .build()
                        .start());

        channel =
                grpcCleanup.register(
                        InProcessChannelBuilder.forName(serverName).directExecutor().build());

        blockingStub = EventsServiceGrpc.newBlockingStub(channel);
        asyncStub = EventsServiceGrpc.newStub(channel);
    }

    @Test
    public void testUnregisterNoAirline() {
        StatusRuntimeException exception =
                Assertions.assertThrows(
                        StatusRuntimeException.class,
                        () -> blockingStub.unregister(UnregisterRequest.newBuilder().build()));

        Assertions.assertEquals(Status.INVALID_ARGUMENT.getCode(), exception.getStatus().getCode());
        Assertions.assertEquals(
                "An airline has to be specified", exception.getStatus().getDescription());
    }

    @Test
    public void testUnregisterNotFound() throws NoSuchElementException {
        doThrow(NoSuchElementException.class).when(eventManager).unregister("Iberia");

        StatusRuntimeException exception =
                Assertions.assertThrows(
                        StatusRuntimeException.class,
                        () ->
                                blockingStub.unregister(
                                        UnregisterRequest.newBuilder()
                                                .setAirline("Iberia")
                                                .build()));

        Assertions.assertEquals(Status.NOT_FOUND.getCode(), exception.getStatus().getCode());
        Assertions.assertEquals(
                "This airline is not registered for events",
                exception.getStatus().getDescription());
    }

    @Test
    public void testUnregister() throws NoSuchElementException {
        Assertions.assertDoesNotThrow(
                () ->
                        blockingStub.unregister(
                                UnregisterRequest.newBuilder().setAirline("Iberia").build()));
        verify(eventManager).unregister("Iberia");
    }

    @Test
    public void testRegisterNoAirline() {
        final CompletableFuture<Status> errorStatus = new CompletableFuture<>();
        StreamObserver<RegisterResponse> responseObserver =
                new StreamObserver<>() {
                    @Override
                    public void onNext(RegisterResponse value) {
                        // Do nothing
                    }

                    @Override
                    public void onError(Throwable t) {
                        errorStatus.complete(Status.fromThrowable(t));
                    }

                    @Override
                    public void onCompleted() {
                        // Do nothing
                    }
                };

        asyncStub.register(RegisterRequest.newBuilder().build(), responseObserver);
        Status status = errorStatus.join();

        Assertions.assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
        Assertions.assertEquals("An airline has to be specified", status.getDescription());
    }

    @Test
    public void testRegisterAlreadyExists()
            throws AlreadyExistsException, ExecutionException, InterruptedException {

        final CompletableFuture<Status> errorStatus = new CompletableFuture<>();
        StreamObserver<RegisterResponse> responseObserver =
                new StreamObserver<>() {
                    @Override
                    public void onNext(RegisterResponse value) {
                        // Do nothing
                    }

                    @Override
                    public void onError(Throwable t) {
                        errorStatus.complete(Status.fromThrowable(t));
                    }

                    @Override
                    public void onCompleted() {
                        // Do nothing
                    }
                };

        when(passengerRepository.hasAirline("Iberia")).thenReturn(true);
        doThrow(AlreadyExistsException.class)
                .when(eventManager)
                .register(any(String.class), any(StreamObserver.class));

        asyncStub.register(
                RegisterRequest.newBuilder().setAirline("Iberia").build(), responseObserver);

        Status status = errorStatus.get();

        Assertions.assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
        Assertions.assertEquals(
                "This airline has already been registered for events", status.getDescription());
    }

    @Test
    public void testRegisterNoExpectedPassengers() {

        final CompletableFuture<Status> errorStatus = new CompletableFuture<>();
        StreamObserver<RegisterResponse> responseObserver =
                new StreamObserver<>() {
                    @Override
                    public void onNext(RegisterResponse value) {
                        // Do nothing
                    }

                    @Override
                    public void onError(Throwable t) {
                        errorStatus.complete(Status.fromThrowable(t));
                    }

                    @Override
                    public void onCompleted() {
                        // Do nothing
                    }
                };
        when(passengerRepository.hasAirline("Iberia")).thenReturn(false);

        asyncStub.register(
                RegisterRequest.newBuilder().setAirline("Iberia").build(), responseObserver);

        Status status = errorStatus.join();

        Assertions.assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
        Assertions.assertEquals(
                "This airline has no flights nor passenger scheduled for this airport",
                status.getDescription());
    }

    @Test
    public void testRegister() throws AlreadyExistsException {
        CompletableFuture<EventType> eventType = new CompletableFuture<>();
        StreamObserver<RegisterResponse> responseObserver =
                new StreamObserver<>() {
                    @Override
                    public void onNext(RegisterResponse value) {
                        eventType.complete(value.getEventType());
                    }

                    @Override
                    public void onError(Throwable t) {
                        // Do nothing
                    }

                    @Override
                    public void onCompleted() {
                        // Do nothing
                    }
                };

        when(passengerRepository.hasAirline("Iberia")).thenReturn(true);
        doNothing().when(eventManager).register(any(String.class), any(StreamObserver.class));

        asyncStub.register(
                RegisterRequest.newBuilder().setAirline("Iberia").build(), responseObserver);
        EventType type = eventType.join();

        // Only way I found to close the grpc connection
        channel.shutdownNow();

        Assertions.assertEquals(EventType.EVENT_TYPE_AIRLINE_REGISTERED, type);
    }
}
