package ar.edu.itba.pod.server.queues;

import ar.edu.itba.pod.server.models.Range;

import java.util.Map;
import java.util.Optional;

public interface PassengerQueue {

    Map<Range, Integer> getPassengersInAllCounters();

    Optional<Integer> getPassengersInCounter(Range counterRange);
}
