package ar.edu.itba.pod.server.queues;

import ar.edu.itba.pod.server.models.Range;

import java.util.Map;

public interface PassengerQueue {

    Map<Range, Integer> getPassengersInCounterRange();
}
