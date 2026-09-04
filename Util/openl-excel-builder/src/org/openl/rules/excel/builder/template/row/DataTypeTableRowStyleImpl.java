package org.openl.rules.excel.builder.template.row;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;

@RequiredArgsConstructor
public class DataTypeTableRowStyleImpl implements DataTypeRowStyle {

    @Getter
    private final CellStyle typeStyle;
    @Getter
    private final CellStyle nameStyle;
    private final CellStyle defaultValueStyle;

    @Override
    public CellStyle getValueStyle() {
        return defaultValueStyle;
    }
}
