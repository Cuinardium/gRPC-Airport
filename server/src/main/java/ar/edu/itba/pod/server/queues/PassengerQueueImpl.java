package ar.edu.itba.pod.server.queues;

import ar.edu.itba.pod.server.models.Range;

import java.util.Map;

public class PassengerQueueImpl implements PassengerQueue {
    @Override
    public Map<Range, Integer> getPassengersInQueuePerRange() {
        return Map.of();
    }
}
