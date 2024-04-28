package ar.edu.itba.pod.server.models;

import ar.edu.itba.pod.grpc.common.CounterRange;
import ar.edu.itba.pod.grpc.counter.SectorInfo;

import java.util.*;

public final class Sector {

    private final String sectorName;
    private final List<CountersRange> countersRangeList;

    public Sector(String sectorName, List<CountersRange> countersRange) {
        this.sectorName = sectorName;
        this.countersRangeList = countersRange;
    }

    public Sector fromEntry(Map.Entry<String, Set<CountersRange>> entry) {
        return new Sector(entry.getKey(), entry.getValue().stream().toList());
    }

    public String sectorName() {
        return sectorName;
    }

    public List<CountersRange> countersRangeList() {
        return countersRangeList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sector sector)) return false;
        return Objects.equals(sectorName, sector.sectorName) && Objects.equals(countersRangeList, sector.countersRangeList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sectorName, countersRangeList);
    }

    public SectorInfo grpcMessage() {
        return SectorInfo
                .newBuilder()
                .setSectorName(this.sectorName)
                .addAllCounterRanges(this.countersRangeList.stream().map(
                                counterRange -> CounterRange.newBuilder()
                                        .setFrom(counterRange.range().from())
                                        .setTo(counterRange.range().to())
                                        .build())
                        .toList())
                .build();
    }
}
