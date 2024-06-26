package ar.edu.itba.pod.server.models;

import ar.edu.itba.pod.grpc.common.CounterRange;

import java.util.Objects;
import java.util.Optional;

public final class CountersRange implements Comparable<CountersRange> {
    private final Range range;
    private final AssignedInfo assignedInfo;

    public CountersRange(Range range, AssignedInfo assignedInfo) {
        this.range = range;
        this.assignedInfo = assignedInfo;
    }

    public CountersRange(Range range) {
        this(range, null);
    }

    public Range range() {
        return range;
    }

    public Optional<AssignedInfo> assignedInfo() {
        return Optional.ofNullable(assignedInfo);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (CountersRange) obj;
        return Objects.equals(this.range, that.range)
                && Objects.equals(this.assignedInfo, that.assignedInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(range, assignedInfo);
    }

    @Override
    public String toString() {
        return "CountersRange[" + "range=" + range + ", " + "assignedInfo=" + assignedInfo + ']';
    }

    public CounterRange grpcMessage(CountersRange countersRange) {
        return CounterRange
                .newBuilder()
                .setFrom(this.range.from())
                .setTo(this.range.to())
                .build();
    }

    @Override
    public int compareTo(CountersRange other) {
        if(this.range.equals(other.range)) {
            return 0;
        }
        if(this.range.from() < other.range.from()) {
            return -1;
        }
        return 1;
    }
}
