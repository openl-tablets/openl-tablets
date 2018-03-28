package org.openl.excel.grid;

import java.util.*;

import org.openl.excel.parser.AlignedValue;
import org.openl.excel.parser.MergedCell;
import org.openl.excel.parser.ExtendedValue;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.*;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.util.StringUtils;

public class ParsedGrid extends AGrid {
    private final Object[][] cells;
    private final String uri;
    private final int firstRowNum;
    private final int firstColNum;
    private final boolean use1904Windowing;
    private List<IGridRegion> regions = new ArrayList<>();

    private Map<CellKey, CellMetaInfo> metaInfoMap = new HashMap<>();

    ParsedGrid(Object[][] cells, String uri, int firstRowNum, int firstColNum, boolean use1904Windowing) {
        this.cells = cells;
        this.uri = uri;
        this.firstRowNum = firstRowNum;
        this.firstColNum = firstColNum;
        this.use1904Windowing = use1904Windowing;

        findRegions();
    }

    @Override
    public ICell getCell(int column, int row) {
        return new ParsedCell(row, column, this);
    }

    @Override
    public int getColumnWidth(int i) {
        return 0;
    }

    @Override
    public int getMaxColumnIndex(int row) {
        if (cells.length <= row) {
            return 0;
        }
        return cells[row].length - 1;
    }

    @Override
    public int getMaxRowIndex() {
        return cells.length - 1;
    }

    @Override
    public IGridRegion getMergedRegion(int i) {
        return regions.get(i);
    }

    @Override
    public int getMinColumnIndex(int i) {
        return 0;
    }

    @Override
    public int getMinRowIndex() {
        return 0;
    }

    @Override
    public int getNumberOfMergedRegions() {
        return regions.size();
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public boolean isEmpty(int col, int row) {
        Object value = getCellValue(row, col);
        return value == null || value instanceof String && StringUtils.isBlank((String) value);
    }

    private void findRegions() {
        // This algorithm can be improved. Feel free to modify it if it becomes bottleneck.
        LinkedHashSet<CellRowCol> startPoints = new LinkedHashSet<>();

        // Find top left points
        for (int i = 0; i < cells.length; i++) {
            Object[] row = cells[i];
            for (int j = 0; j < row.length; j++) {
                Object col = row[j];

                if (col instanceof MergedCell) {
                    CellRowCol rowCol = findTopLeft(i, j);
                    startPoints.add(rowCol);
                }
            }
        }

        // Find bottom right points and create regions
        for (CellRowCol start : startPoints) {
            CellRowCol end = findBottomRight(start.row, start.col);
            regions.add(new GridRegion(start.row, start.col, end.row, end.col));
        }
    }

    private CellRowCol findTopLeft(int i, int j) {
        while (cells[i][j] == MergedCell.MERGE_WITH_LEFT) {
            j--;
        }
        while (cells[i][j] == MergedCell.MERGE_WITH_UP) {
            i--;
        }
        return new CellRowCol(i, j);
    }

    private CellRowCol findBottomRight(int row, int col) {
        int endRow = row;
        int endCol = col;
        while (endRow < cells.length - 1 && cells[endRow + 1][endCol] == MergedCell.MERGE_WITH_UP) {
            endRow++;
        }
        while (endCol < cells[endRow].length - 1 && cells[endRow][endCol + 1] == MergedCell.MERGE_WITH_LEFT) {
            endCol++;
        }

        return new CellRowCol(endRow, endCol);
    }

    /////////////////////////// Methods used in ParsedCell ///////////////////////////////////

    protected Object getCellValue(int row, int column) {
        if (cells.length <= row || cells[row].length <= column) {
            return null;
        }

        Object value = cells[row][column];
        if (value instanceof MergedCell) {
            CellRowCol topLeft = findTopLeft(row, column);
            value = cells[topLeft.row][topLeft.col];
        }
        if (value instanceof ExtendedValue) {
            value = ((ExtendedValue) value).getValue();
        }

        return value;
    }

    protected ICellStyle getCellStyle(int row, int col) {
        if (cells.length <= row || cells[row].length <= col) {
            return null;
        }

        Object value = cells[row][col];
        if (value instanceof AlignedValue) {
            return new IndentedStyle(((AlignedValue) value).getIndent());
        }

        return null;
    }

    protected IGridRegion getRegion(int row, int col) {
        for (IGridRegion region : regions) {
            if (region.getTop() <= row && row <= region.getBottom() &&
                    region.getLeft() <= col && col <= region.getRight()) {
                return region;
            }
        }

        return null;
    }

    protected CellMetaInfo getCellMetaInfo(int col, int row) {
        CellKey ck = CellKey.CellKeyFactory.getCellKey(col, row);
        return metaInfoMap.get(ck);
    }

    protected synchronized void setCellMetaInfo(int col, int row, CellMetaInfo meta) {
        CellKey ck = CellKey.CellKeyFactory.getCellKey(col, row);
        if (meta == null) {
            metaInfoMap.remove(ck);
        } else {
            metaInfoMap.put(ck, meta);
        }
    }

    protected int getFirstRowNum() {
        return firstRowNum;
    }

    protected int getFirstColNum() {
        return firstColNum;
    }

    protected boolean isUse1904Windowing() {
        return use1904Windowing;
    }

    //////////////////////////////////////////////////////////////////////

    private static class CellRowCol {
        final int row;
        final int col;

        private CellRowCol(int row, int col) {
            this.row = row;
            this.col = col;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            CellRowCol that = (CellRowCol) o;
            return row == that.row &&
                    col == that.col;
        }

        @Override
        public int hashCode() {
            return Objects.hash(row, col);
        }
    }

}
