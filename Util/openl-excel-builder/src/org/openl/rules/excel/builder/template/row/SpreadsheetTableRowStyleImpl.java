package org.openl.rules.excel.builder.template.row;

import org.apache.poi.ss.usermodel.CellStyle;

public class SpreadsheetTableRowStyleImpl extends NameValueRowStyleImpl {

    public SpreadsheetTableRowStyleImpl(CellStyle nameStyle, CellStyle valueStyle) {
        super(nameStyle, valueStyle);
    }
}
