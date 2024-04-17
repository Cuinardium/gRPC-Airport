package ar.edu.itba.pod.server.models;

import java.util.List;

public record AssignedInfo(String airline, List<String> flights, int passengersInQueue) {}
