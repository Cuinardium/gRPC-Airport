package ar.edu.itba.pod.server.models;

import java.util.List;

public record Assignment(String airline, List<String> flights, int counterCount, Runnable onMoved, Runnable onAssigned) {}
