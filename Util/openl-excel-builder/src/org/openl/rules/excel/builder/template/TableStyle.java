package org.openl.rules.excel.builder.template;

import org.openl.rules.excel.builder.CellRangeSettings;
import org.openl.rules.table.ui.ICellStyle;

public interface TableStyle {

    String getHeaderTemplate();

    CellRangeSettings getHeaderSettings();

    ICellStyle getHeaderStyle();

}
