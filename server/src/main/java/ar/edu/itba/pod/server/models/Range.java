package ar.edu.itba.pod.server.models;

import java.util.ArrayList;
import java.util.List;

public record Range(int from, int to) {

    public static List<Range> mergeRanges(List<Range> ranges) {
        List<Range> resp = new ArrayList<>();

        for (Range range : ranges) {
            int currentFrom = range.from();

            if (resp.isEmpty()) {
                resp.add(range);
                continue;
            }

            int previousTo = resp.get(resp.size() - 1).to();

            if (currentFrom == previousTo + 1) {
                resp.set(resp.size() - 1, new Range(resp.get(resp.size() - 1).from(), range.to()));
            } else {
                resp.add(range);
            }
        }

        return resp;
    }
}
