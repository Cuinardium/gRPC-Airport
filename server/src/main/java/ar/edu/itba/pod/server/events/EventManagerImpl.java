package ar.edu.itba.pod.server.events;

import ar.edu.itba.pod.grpc.events.RegisterResponse;

import ar.edu.itba.pod.server.exceptions.AlreadyExistsException;
import io.grpc.stub.StreamObserver;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

public class EventManagerImpl implements EventManager {

    private final Map<String, StreamObserver<RegisterResponse>> streams = new ConcurrentHashMap<>();

    // TODO: checkear si se deben sincronizar los llamados a los stream observers

    @Override
    public void register(String airline, StreamObserver<RegisterResponse> eventStream) throws AlreadyExistsException {
        if (streams.containsKey(airline)) {
            throw new AlreadyExistsException("This airline is already registered for events");
        }

        streams.put(airline, eventStream);
    }

    @Override
    public void unregister(String airline) throws NoSuchElementException {
        if (!streams.containsKey(airline)) {
            throw new NoSuchElementException("This airline is not registered for events");
        }

        StreamObserver<RegisterResponse> airlineStream = streams.get(airline);

        synchronized (airlineStream) {
            // Alguien ya lo borro, TODO: Nose si va a pasar
            if (!streams.containsKey(airline)) {
                return;
            }

            // Corto el stream
            airlineStream.onCompleted();

            // Saco a la aerolinea
            streams.remove(airline);
        }
    }

    @Override
    public boolean notify(String airline, RegisterResponse event) {
        if (!streams.containsKey(airline)) {
            return false;
        }

        StreamObserver<RegisterResponse> airlineStream = streams.get(airline);

        synchronized (airlineStream) {
            // Alguien ya lo borro
            if (!streams.containsKey(airline)) {
                return false;
            }

            airlineStream.onNext(event);
        }

        return true;
    }
}
