package ar.edu.itba.pod.server.events;

import ar.edu.itba.pod.grpc.events.RegisterResponse;
import ar.edu.itba.pod.server.exceptions.AlreadyExistsException;
import ar.edu.itba.pod.server.exceptions.NotFoundException;
import io.grpc.stub.StreamObserver;

public interface EventManager {

    void register(String airline, StreamObserver<RegisterResponse> eventStream) throws AlreadyExistsException;
    void unregister(String airline) throws NotFoundException;

    void notify(String airline, RegisterResponse event);

}
