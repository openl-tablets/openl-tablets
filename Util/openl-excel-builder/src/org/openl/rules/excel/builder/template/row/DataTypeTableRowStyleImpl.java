package org.openl.rules.excel.builder.template.row;

import org.apache.poi.ss.usermodel.CellStyle;

public class DataTypeTableRowStyleImpl implements DataTypeRowStyle {

    private final CellStyle typeStyle;
    private final CellStyle nameStyle;
    private final CellStyle defaultValueStyle;

    public DataTypeTableRowStyleImpl(CellStyle typeStyle, CellStyle nameStyle, CellStyle defaultValueStyle) {
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

    public CellStyle getValueStyle() {
        return defaultValueStyle;
    }
}
