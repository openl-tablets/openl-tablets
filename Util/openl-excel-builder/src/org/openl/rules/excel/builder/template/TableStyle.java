package org.openl.rules.excel.builder.template;

import org.apache.poi.ss.usermodel.CellStyle;
import org.openl.rules.excel.builder.CellRangeSettings;
import org.openl.rules.table.ui.ICellStyle;

public interface TableStyle {

    String getHeaderTemplate();

    CellRangeSettings getHeaderSettings();

    CellStyle getHeaderStyle();
}
