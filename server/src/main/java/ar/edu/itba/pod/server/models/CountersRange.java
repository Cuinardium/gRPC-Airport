package ar.edu.itba.pod.server.models;

import java.util.List;

public record CountersRange(
        String sector, Range range, String airline, List<String> flights) {}
