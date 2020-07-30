package org.openl.rules.excel.builder.template;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;

public interface DataTypeTableStyle extends TableStyle {

    CellStyle getDateFieldStyle();

    Font getHeaderFont();

}
