package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.grpc.events.*;
import ar.edu.itba.pod.server.events.EventManager;
import ar.edu.itba.pod.server.exceptions.AlreadyExistsException;
import ar.edu.itba.pod.server.exceptions.NotFoundException;
import ar.edu.itba.pod.server.repositories.PassengerRepository;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;

import org.junit.Rule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.mockito.Mockito.*;

public class EventServiceTest {

    @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    private final EventManager eventManager = mock(EventManager.class);
    private final PassengerRepository passengerRepository = mock(PassengerRepository.class);

    private EventsServiceGrpc.EventsServiceBlockingStub blockingStub;
    private EventsServiceGrpc.EventsServiceStub asyncStub;

    @BeforeEach
    public final void setUp() throws Exception {
        String serverName = InProcessServerBuilder.generateName();

        EventsService eventsService = new EventsService(passengerRepository, eventManager);

        grpcCleanup.register(
                InProcessServerBuilder.forName(serverName)
                        .directExecutor()
                        .addService(eventsService)
                        .build()
                        .start());

        blockingStub =
                EventsServiceGrpc.newBlockingStub(
                        grpcCleanup.register(
                                InProcessChannelBuilder.forName(serverName)
                                        .directExecutor()
                                        .build()));
        asyncStub =
                EventsServiceGrpc.newStub(
                        grpcCleanup.register(
                                InProcessChannelBuilder.forName(serverName)
                                        .directExecutor()
                                        .build()));
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
    public void testUnregisterNotFound() throws NotFoundException {
        doThrow(NotFoundException.class).when(eventManager).unregister("Iberia");

        StatusRuntimeException exception =
                Assertions.assertThrows(
                        StatusRuntimeException.class,
                        () ->
                                blockingStub.unregister(
                                        UnregisterRequest.newBuilder().setAirline("Iberia").build()));

        Assertions.assertEquals(Status.NOT_FOUND.getCode(), exception.getStatus().getCode());
        Assertions.assertEquals(
                "This airline is not registered for events", exception.getStatus().getDescription());
    }

    @Test
    public void testUnregister() throws NotFoundException {
        Assertions.assertDoesNotThrow(() -> blockingStub.unregister(UnregisterRequest.newBuilder().setAirline("Iberia").build()));
        verify(eventManager).unregister("Iberia");
    }

    @Test
    public void testRegisterNoAirline() {
        final CompletableFuture<Status> errorStatus = new CompletableFuture<>();
        StreamObserver<RegisterResponse> responseObserver = new StreamObserver<>() {
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
        Assertions.assertEquals(
                "An airline has to be specified", status.getDescription());
    }

    @Test
    public void testRegisterAlreadyExists() throws AlreadyExistsException, ExecutionException, InterruptedException {

        final CompletableFuture<Status> errorStatus = new CompletableFuture<>();
        StreamObserver<RegisterResponse> responseObserver = new StreamObserver<>() {
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
        doThrow(AlreadyExistsException.class).when(eventManager).register(any(String.class), any(StreamObserver.class));

        asyncStub.register(RegisterRequest.newBuilder().setAirline("Iberia").build(), responseObserver);

        Status status = errorStatus.get();


        Assertions.assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
        Assertions.assertEquals(
                "This airline has already been registered for events", status.getDescription());
    }

    @Test
    public void testRegisterNoExpectedPassengers() {

        final CompletableFuture<Status> errorStatus = new CompletableFuture<>();
        StreamObserver<RegisterResponse> responseObserver = new StreamObserver<>() {
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

        asyncStub.register(RegisterRequest.newBuilder().setAirline("Iberia").build(), responseObserver);

        Status status = errorStatus.join();

        Assertions.assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
        Assertions.assertEquals(
                "This airline has no flights nor passenger scheduled for this airport", status.getDescription());

    }

    @Test
    public void testRegister() throws AlreadyExistsException {
        CompletableFuture<EventType> eventType = new CompletableFuture<>();
        StreamObserver<RegisterResponse> responseObserver = new StreamObserver<>() {
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

        asyncStub.register(RegisterRequest.newBuilder().setAirline("Iberia").build(), responseObserver);
        EventType type = eventType.join();

        Assertions.assertEquals(EventType.EVENT_TYPE_AIRLINE_REGISTERED, type);
    }

}
