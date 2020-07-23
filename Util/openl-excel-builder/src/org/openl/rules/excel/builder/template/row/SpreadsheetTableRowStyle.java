package org.openl.rules.excel.builder.template.row;

import org.apache.poi.ss.usermodel.CellStyle;

public class SpreadsheetTableRowStyle {

    private CellStyle nameStyle;
    private CellStyle valueStyle;

    public SpreadsheetTableRowStyle(CellStyle nameStyle, CellStyle valueStyle) {
        this.nameStyle = nameStyle;
        this.valueStyle = valueStyle;
    }

    public CellStyle getNameStyle() {
        return nameStyle;
    }

    public CellStyle getValueStyle() {
        return valueStyle;
    }
}
