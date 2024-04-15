package ar.edu.itba.pod.server.events;

import ar.edu.itba.pod.grpc.events.RegisterResponse;
import io.grpc.stub.StreamObserver;

public interface EventManager {

    void register(String airline, StreamObserver<RegisterResponse> eventStream);
    void unregister(String airline);

    boolean isRegistered(String airline);

    void notify(String airline, RegisterResponse event);

}
