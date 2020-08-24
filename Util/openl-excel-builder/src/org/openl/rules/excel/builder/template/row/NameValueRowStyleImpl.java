package org.openl.rules.excel.builder.template.row;

import org.apache.poi.ss.usermodel.CellStyle;

public class NameValueRowStyleImpl implements NameValueRowStyle {

    private final CellStyle nameStyle;
    private final CellStyle valueStyle;

    public NameValueRowStyleImpl(CellStyle nameStyle, CellStyle valueStyle) {
        this.nameStyle = nameStyle;
        this.valueStyle = valueStyle;
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
