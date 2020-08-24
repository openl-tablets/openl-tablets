package org.openl.rules.excel.builder.template;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.openl.rules.excel.builder.template.row.DataTypeRowStyle;

public interface DataTypeTableStyle extends TableStyle {

    CellStyle getDateFieldStyle();

    Font getHeaderFont();

    @Override
    DataTypeRowStyle getRowStyle();

    @Override
    DataTypeRowStyle getLastRowStyle();
}
