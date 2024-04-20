package ar.edu.itba.pod.server.models;

import java.util.List;
import java.util.Objects;

public final class Sector {

    private final String sectorName;
    private final List<CountersRange> countersRangeList;

    public Sector(String sectorName, List<CountersRange> countersRange) {
        this.sectorName = sectorName;
        this.countersRangeList = countersRange;
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
}
