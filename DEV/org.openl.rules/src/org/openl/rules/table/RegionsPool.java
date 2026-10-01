package org.openl.rules.table;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import lombok.RequiredArgsConstructor;

/**
 * Regions pool that gives region containing some cell quickly.
 *
 * @author PUdalau
 */
public class RegionsPool {
    @RequiredArgsConstructor
    private static class DisjointInterval {
        /**
         * Orders the intervals by their positions. Two intervals that intersect are equal.
         */
        static final Comparator<DisjointInterval> BY_POSITION = (a, b) -> {
            if (a.right < b.left) {
                return -1;
            }
            if (a.left > b.right) {
                return 1;
            }
            return 0;
        };

        private final int left;
        private final int right;
    }

    /**
     * Fast regions pool. This is map that gives for each row another map that contains disjoint intervals covered by
     * some region.
     */
    private final Map<Integer, Map<DisjointInterval, IGridRegion>> pool = new HashMap<>();

    /**
     * Instantiates the pool.
     *
     * @param regions All regions to register.
     */
    public RegionsPool(List<IGridRegion> regions) {
        if (regions != null) {
            for (IGridRegion region : regions) {
                add(region);
            }
        }
    }

    public RegionsPool() {
    }

    /**
     * @param region Region to register in the pool
     */
    public void add(IGridRegion region) {
        for (var row = region.getTop(); row <= region.getBottom(); row++) {
            Map<DisjointInterval, IGridRegion> regionsMap = pool.computeIfAbsent(row, k -> new TreeMap<>(DisjointInterval.BY_POSITION));
            regionsMap.put(new DisjointInterval(region.getLeft(), region.getRight()), region);
        }
    }

    public void remove(IGridRegion region) {
        if (region != null) {
            for (var row = region.getTop(); row <= region.getBottom(); row++) {
                Map<DisjointInterval, IGridRegion> regionsMap = pool.get(row);
                regionsMap.remove(new DisjointInterval(region.getLeft(), region.getRight()));
                if (regionsMap.isEmpty()) {
                    pool.remove(row);
                }
            }
        }
    }

    /**
     * removes registered region containing specified coordinates.
     */
    public void remove(int column, int row) {
        var region = getRegionContaining(column, row);
        remove(region);
    }

    /**
     * @return returns registered region containing specified coordinates.
     */
    public IGridRegion getRegionContaining(int column, int row) {
        Map<DisjointInterval, IGridRegion> regionsMap = pool.get(row);
        if (regionsMap != null) {
            return regionsMap.get(new DisjointInterval(column, column));
        }
        return null;
    }
}
