package org.openl.rules.table;

public abstract class AGrid implements IGrid {

    public static final char RANGE_SEPARATOR = ':';

    @Override
    public IGridTable[] getTables() {
        return new GridSplitter(this).split();
    }

    /**
     * Gets the URI to the table by its four coordinates on the sheet.
     *
     * @return URI to the table in the sheet. (e.g. <code>file:D:\work\Workspace\org.openl.tablets.tutorial4\rules
     * \main&wbName=Tutorial_4.xls&wsName=Vehicle-Scoring&range=B3:D12</code>)
     */
    @Override
    public String getRangeUri(int colStart, int rowStart, int colEnd, int rowEnd) {

        if (colStart == colEnd && rowStart == rowEnd) {
            var cell = getCell(colStart, rowStart);
            var region = cell.getRegion();
            if (region == null || (region.getRight() == region.getLeft() && region.getBottom() == region.getTop())) {
                return getUri() + "&cell=" + cell.getUri();
            } else {
                var range = getCell(region.getLeft(), region.getTop())
                        .getUri() + RANGE_SEPARATOR + getCell(region.getRight(), region.getBottom()).getUri();
                return getUri() + "&range=" + range;
            }
        }

        var range = getCell(colStart, rowStart).getUri() + RANGE_SEPARATOR + getCell(colEnd, rowEnd).getUri();
        return getUri() + "&range=" + range;
    }

    @Override
    public IGridRegion getRegionContaining(int col, int row) {
        var nRegions = getNumberOfMergedRegions();
        for (var i = 0; i < nRegions; i++) {
            var reg = getMergedRegion(i);
            if (IGridRegion.Tool.contains(reg, col, row)) {
                return reg;
            }
        }
        return null;
    }

    @Override
    public IGridRegion getRegionStartingAt(int colFrom, int rowFrom) {
        var reg = getRegionContaining(colFrom, rowFrom);
        if (reg != null && reg.getLeft() == colFrom && reg.getTop() == rowFrom) {
            return reg;
        }
        return null;
    }

    @Override
    public boolean isPartOfTheMergedRegion(int x, int y) {
        return getRegionContaining(x, y) != null;
    }

    @Override
    public boolean isTopLeftCellInMergedRegion(int column, int row) {
        return getRegionStartingAt(column, row) != null;
    }

    @Override
    public boolean isInOneMergedRegion(int firstCellColumn, int firstCellRow, int secondCellColumn, int secondCellRow) {
        var region = getRegionContaining(firstCellColumn, firstCellRow);
        return region != null && IGridRegion.Tool
                .contains(region, secondCellColumn, secondCellRow);
    }
}
