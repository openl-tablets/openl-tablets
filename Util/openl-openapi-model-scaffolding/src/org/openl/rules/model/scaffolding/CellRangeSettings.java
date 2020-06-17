package org.openl.rules.model.scaffolding;

import org.apache.poi.ss.util.CellRangeAddress;

public class CellRangeSettings {
    private int height;
    private int width;

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
