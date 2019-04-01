package org.openl.rules.table;

import org.openl.rules.table.xls.XlsUrlParser;

/**
 * Default implementation for grid tables.
 *
 * @author snshor
 */
public abstract class AGridTable implements IGridTable {

    private XlsUrlParser cacheUrlParser;

    @Override
    public IGridRegion getRegion() {
        int left = getGridColumn(0, 0);
        int top = getGridRow(0, 0);

        int right = -1;
        int bottom = -1;

        if (isNormalOrientation()) {
            right = getGridColumn(getWidth() - 1, 0);
            bottom = getGridRow(0, getHeight() - 1);
        } else {
            right = getGridColumn(0, getHeight() - 1);
            bottom = getGridRow(getWidth() - 1, 0);
        }

        return new GridRegion(top, left, bottom, right);
    }

    @Override
    public String getUri() {
        int w = getWidth();
        int h = getHeight();
        return getGrid()
            .getRangeUri(getGridColumn(0, 0), getGridRow(0, 0), getGridColumn(w - 1, h - 1), getGridRow(w - 1, h - 1));
    }

    @Override
    public XlsUrlParser getUriParser() {
        if (cacheUrlParser == null) {
            cacheUrlParser = new XlsUrlParser(getUri());
        }
        return cacheUrlParser;
    }

    @Override
    public String getUri(int col, int row) {
        int colStart = getGridColumn(col, row);
        int rowStart = getGridRow(col, row);
        return getGrid().getRangeUri(colStart, rowStart, colStart, rowStart);
    }

    @Override
    public IGridTable transpose() {
        return new TransposedGridTable(this);
    }

    @Override
    public ICell getCell(int column, int row) {
        return new GridTableCell(column, row, this);
    }

    @Override
    public IGridTable getColumn(int column) {
        return getColumns(column, column);
    }

    @Override
    public IGridTable getColumns(int from) {
        return getColumns(from, getWidth() - 1);
    }

    @Override
    public IGridTable getColumns(int from, int to) {
        int colsNum = to - from + 1;
        return getSubtable(from, 0, colsNum, getHeight());
    }

    @Override
    public IGridTable getRow(int row) {
        return getRows(row, row);
    }

    @Override
    public IGridTable getRows(int from) {
        return getRows(from, getHeight() - 1);
    }

    @Override
    public IGridTable getRows(int from, int to) {
        int rowsNum = to - from + 1;
        return getSubtable(0, from, getWidth(), rowsNum);
    }

    @Override
    public IGridTable getSubtable(int column, int row, int width, int height) {
        if (width == 0 || height == 0) {
            return null;
        }
        if (getWidth() == width && getHeight() == height) {
            return this;
        }

        if (width == 1 && height == 1) {
            return new SingleCellGridTable(this, column, row);
        }

        return new SubGridTable(this, column, row, width, height);
    }

    @Override
    public String toString() {
        StringBuilder tableVizualization = new StringBuilder();
        tableVizualization.append(super.toString())
            .append(isNormalOrientation() ? "[N]" : "[T]")
            .append("(")
            .append(getWidth())
            .append(" x ")
            .append(getHeight())
            .append(")")
            .append(getRegion().toString())
            .append("\n");
        for (int i = 0; i < getHeight(); i++) {
            int length = 0;
            for (int j = 0; j < getWidth(); j++) {
                String strValue = getCell(j, i).getStringValue();
                if (strValue == null) {
                    strValue = "EMPTY";
                }
                length += strValue.length();
                tableVizualization.append(strValue);
                tableVizualization.append("|");
            }
            tableVizualization.append("\n");
            for (int k = 0; k <= length; k++) {
                tableVizualization.append("-");
            }
            tableVizualization.append("\n");
        }

        return tableVizualization.toString();
    }

}
