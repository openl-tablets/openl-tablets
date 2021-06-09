package org.openl.rules.excel.builder.template.row;

import org.apache.poi.ss.usermodel.CellStyle;

public interface NameValueRowStyle {

    CellStyle getNameStyle();

    CellStyle getValueStyle();
}
