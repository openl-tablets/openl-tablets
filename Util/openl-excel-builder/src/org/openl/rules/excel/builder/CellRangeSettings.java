package org.openl.rules.excel.builder;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.util.CellRangeAddress;

@RequiredArgsConstructor
public class CellRangeSettings {
    private final int height;
    private final int width;

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
