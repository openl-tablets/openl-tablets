package org.openl.rules.excel.builder.template.row;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;

@RequiredArgsConstructor
public class NameValueRowStyleImpl implements NameValueRowStyle {

    private final CellStyle nameStyle;
    private final CellStyle valueStyle;

    @Override
    public CellStyle getNameStyle() {
        return nameStyle;
    }

    @Override
    public CellStyle getValueStyle() {
        return valueStyle;
    }
}
