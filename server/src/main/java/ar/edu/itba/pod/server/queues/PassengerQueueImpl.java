package ar.edu.itba.pod.server.queues;

import ar.edu.itba.pod.server.models.Range;

import java.util.Map;
import java.util.Optional;

public class PassengerQueueImpl implements PassengerQueue {

    @Override
    public void addPassengerToQueue(Range counterRange, String booking) {

    }

    @Override
    public Map<Range, Integer> getPassengersInAllCounters() {
        return Map.of();
    }

    @Override
    public Optional<Integer> getPassengersInCounter(Range counterRange) {
        return Optional.empty();
    }

    @Override
    public boolean hasPassengerInCounter(Range counterRange, String booking) {
        return false;
    }
}
