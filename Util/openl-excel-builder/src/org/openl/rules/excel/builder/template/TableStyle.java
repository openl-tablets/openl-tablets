package org.openl.rules.excel.builder.template;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.RichTextString;
import org.openl.rules.excel.builder.CellRangeSettings;
import org.openl.rules.excel.builder.template.row.NameValueRowStyle;

public interface TableStyle {

    CellStyle getHeaderStyle();

    RichTextString getHeaderTemplate();

    CellRangeSettings getHeaderSizeSettings();

    NameValueRowStyle getRowStyle();

    NameValueRowStyle getLastRowStyle();
}
