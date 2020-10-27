package org.openl.rules.excel.builder.template;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;

public interface DataTableStyle extends TableStyle {

    Font getTypeFont();

    Font getTableNameFont();

    CellStyle getSubheaderStyle();

    CellStyle getColumnHeaderStyle();
}
