package org.openl.rules.excel.builder.template.row;

import org.apache.poi.ss.usermodel.CellStyle;

public class DataTypeTableRowStyle {

    private CellStyle typeStyle;
    private CellStyle nameStyle;
    private CellStyle defaultValueStyle;

    public DataTypeTableRowStyle(CellStyle typeStyle, CellStyle nameStyle, CellStyle defaultValueStyle) {
        this.typeStyle = typeStyle;
        this.nameStyle = nameStyle;
        this.defaultValueStyle = defaultValueStyle;
    }

    public CellStyle getTypeStyle() {
        return typeStyle;
    }

    public CellStyle getNameStyle() {
        return nameStyle;
    }

    public CellStyle getDefaultValueStyle() {
        return defaultValueStyle;
    }
}
