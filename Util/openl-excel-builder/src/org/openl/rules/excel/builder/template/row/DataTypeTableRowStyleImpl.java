package org.openl.rules.excel.builder.template.row;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;

@RequiredArgsConstructor
public class DataTypeTableRowStyleImpl implements DataTypeRowStyle {

    private final CellStyle typeStyle;
    private final CellStyle nameStyle;
    private final CellStyle defaultValueStyle;

    @Override
    public CellStyle getTypeStyle() {
        return typeStyle;
    }

    @Override
    public CellStyle getNameStyle() {
        return nameStyle;
    }

    @Override
    public CellStyle getValueStyle() {
        return defaultValueStyle;
    }
}
