package org.openl.rules.excel.builder;

import org.apache.poi.ss.util.CellRangeAddress;

public class CellRangeSettings {
    private final int height;
    private final int width;

    public CellRangeSettings(int height, int width) {
        this.height = height;
        this.width = width;
    }

    public CellRangeSettings(CellRangeAddress cellRangeAddress) {
        this.height = cellRangeAddress.getLastRow() - cellRangeAddress.getFirstRow();
        this.width = cellRangeAddress.getLastColumn() - cellRangeAddress.getFirstColumn();
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }
}
