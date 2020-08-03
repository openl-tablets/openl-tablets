package org.openl.rules.excel.builder.template.row;

import org.apache.poi.ss.usermodel.CellStyle;

public class SpreadsheetTableRowStyle implements RowStyle {

    private CellStyle nameStyle;
    private CellStyle valueStyle;

    public SpreadsheetTableRowStyle(CellStyle nameStyle, CellStyle valueStyle) {
        this.nameStyle = nameStyle;
        this.valueStyle = valueStyle;
    }

    @Override
    public CellStyle getTypeStyle() {
        return null;
    }

    @Override
    public CellStyle getNameStyle() {
        return nameStyle;
    }

    @Override
    public CellStyle getValueStyle() {
        return valueStyle;
    }
}
