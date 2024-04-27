package ar.edu.itba.pod.server.models;

import java.util.List;

public record PendingAssignment(String airline, List<String> flights, int counterCount) {}
