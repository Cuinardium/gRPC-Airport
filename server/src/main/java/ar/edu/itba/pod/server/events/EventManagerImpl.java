package ar.edu.itba.pod.server.events;

import ar.edu.itba.pod.grpc.events.RegisterResponse;
import io.grpc.stub.StreamObserver;

public class EventManagerImpl implements EventManager {
    @Override
    public void register(String airline, StreamObserver<RegisterResponse> eventStream) {}

    @Override
    public void unregister(String airline) {}

    @Override
    public boolean isRegistered(String airline) {
        return false;
    }

    @Override
    public void notify(String airline, RegisterResponse event) {}
}
