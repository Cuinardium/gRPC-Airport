package ar.edu.itba.pod.server.queues;

import ar.edu.itba.pod.server.models.Range;

import java.util.Map;
import java.util.Optional;

public interface PassengerQueue {

    void addPassengerToQueue(Range counterRange, String booking);

    Map<Range, Integer> getPassengersInAllCounters();

    Optional<Integer> getPassengersInCounter(Range counterRange);

    boolean hasPassengerInCounter(Range counterRange, String booking);
}
