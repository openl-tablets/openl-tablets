package org.openl.rules.table;

public abstract class AGrid implements IGrid {

    public static final String RANGE_SEPARATOR = ":";

    public IGridTable[] getTables() {
        return new GridSplitter(this).split();
    }

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


    public boolean isTopLeftCellInMergedRegion(int column, int row) {
        return getRegionStartingAt(column, row) != null;
    }

    public boolean isInOneMergedRegion(int firstCellColumn, int firstCellRow, int secondCellColumn, int secondCellRow) {
        IGridRegion region = getRegionContaining(firstCellColumn, firstCellRow);
        if (region != null && org.openl.rules.table.IGridRegion.Tool.contains(region, secondCellColumn, secondCellRow)) {
            return true;
        }
        return false;
    }
}
