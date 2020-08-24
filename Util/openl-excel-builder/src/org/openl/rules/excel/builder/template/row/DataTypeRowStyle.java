package org.openl.rules.excel.builder.template.row;

import org.apache.poi.ss.usermodel.CellStyle;

public interface DataTypeRowStyle extends NameValueRowStyle {
    CellStyle getTypeStyle();
}
