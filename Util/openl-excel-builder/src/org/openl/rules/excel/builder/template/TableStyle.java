package org.openl.rules.excel.builder.template;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.RichTextString;
import org.openl.rules.excel.builder.CellRangeSettings;
import org.openl.rules.excel.builder.template.row.RowStyle;

public interface TableStyle {

    CellStyle getHeaderStyle();

    RichTextString getHeaderTemplate();

    CellRangeSettings getHeaderSizeSettings();

    RowStyle getRowStyle();

    RowStyle getLastRowStyle();
}
