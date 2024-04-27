package ar.edu.itba.pod.server.events;

import ar.edu.itba.pod.grpc.events.RegisterResponse;
import ar.edu.itba.pod.server.exceptions.AlreadyExistsException;
import io.grpc.stub.StreamObserver;

import java.util.NoSuchElementException;

public interface EventManager {

    void register(String airline, StreamObserver<RegisterResponse> eventStream) throws AlreadyExistsException;
    void unregister(String airline) throws NoSuchElementException;

    // Returns true if the airline was notified, false otherwise
    boolean notify(String airline, RegisterResponse event);

}
