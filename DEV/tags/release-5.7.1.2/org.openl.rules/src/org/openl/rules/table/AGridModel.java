package org.openl.rules.table;

public abstract class AGridModel implements IGrid {

    public static final String RANGE_SEPARATOR = ":";

    public IGridRegion getRegionContaining(int col, int row) {
        int nregions = getNumberOfMergedRegions();
        for (int i = 0; i < nregions; i++) {
            IGridRegion reg = getMergedRegion(i);
            if (IGridRegion.Tool.contains(reg, col, row)) {
                return reg;
            }
        }
        return null;
    }

    public IGridRegion getRegionStartingAt(int colFrom, int rowFrom) {
        IGridRegion reg = getRegionContaining(colFrom, rowFrom);
        if (reg != null && reg.getLeft() == colFrom && reg.getTop() == rowFrom) {
            return reg;
        }
        return null;
    }

    public boolean isPartOfTheMergedRegion(int x, int y) {
        return getRegionContaining(x, y) != null;
    }

}
