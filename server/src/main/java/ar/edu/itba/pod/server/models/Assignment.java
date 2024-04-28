package ar.edu.itba.pod.server.models;


import java.util.List;
import java.util.function.Consumer;

public record Assignment(String airline, List<String> flights, int counterCount, Consumer<Integer> onMoved, Consumer<Range> onAssigned) {

    public Assignment(String airline, List<String> flights, int counterCount) {
        this(airline, flights, counterCount, (a) -> {}, (a) -> {});
    }

    public Consumer<Integer> getOnMoved() {
        return onMoved;
    }
    public Consumer<Range> getOnAssigned() {
        return onAssigned;
    }
}
