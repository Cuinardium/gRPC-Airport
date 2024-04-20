package ar.edu.itba.pod.server.models;

import java.util.Objects;
import java.util.Optional;

public final class CountersRange {
    private final Range range;
    private final AssignedInfo assignedInfo;

    public CountersRange(String sector, Range range, AssignedInfo assignedInfo) {
        this.range = range;
        this.assignedInfo = assignedInfo;
    }

    public CountersRange(String sector, Range range) {
        this(sector, range, null);
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
        return "CountersRange["
                + "range="
                + range
                + ", "
                + "assignedInfo="
                + assignedInfo
                + ']';
    }
}
