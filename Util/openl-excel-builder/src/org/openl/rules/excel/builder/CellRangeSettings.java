package org.openl.rules.excel.builder;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.util.CellRangeAddress;

@RequiredArgsConstructor
public class CellRangeSettings {
    @Getter
    private final int height;
    @Getter
    private final int width;

    public CellRangeSettings(CellRangeAddress cellRangeAddress) {
        this.height = cellRangeAddress.getLastRow() - cellRangeAddress.getFirstRow();
        this.width = cellRangeAddress.getLastColumn() - cellRangeAddress.getFirstColumn();
    }
}
