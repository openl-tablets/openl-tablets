package org.openl.rules.table;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Regions pool that gives region containing some cell quickly.
 * 
 * @author PUdalau
 */
public class RegionsPool {
    /**
     * Two intervals that intersects are equal.
     */
    private static class DisjointInterval implements Comparable<DisjointInterval> {
        private int left;
        private int right;

        public DisjointInterval(int left, int right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public int compareTo(DisjointInterval o) {
            if (right < o.left) {
                return -1;
            }
            if (left > o.right) {
                return 1;
            }
            return 0;
        }
    }

    /**
     * Fast regions pool. This is map that gives for each row another map that
     * contains disjoint intervals covered by some region.
     */
    private Map<Integer, Map<DisjointInterval, IGridRegion>> pool = new HashMap<>();

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
        for (int row = region.getTop(); row <= region.getBottom(); row++) {
            Map<DisjointInterval, IGridRegion> regionsMap = pool.get(row);
            if (regionsMap == null) {
                regionsMap = new TreeMap<>();
                pool.put(row, regionsMap);
            }
            regionsMap.put(new DisjointInterval(region.getLeft(), region.getRight()), region);
        }
    }

    public void remove(IGridRegion region) {
        if (region != null) {
            for (int row = region.getTop(); row <= region.getBottom(); row++) {
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
        IGridRegion region = getRegionContaining(column, row);
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
