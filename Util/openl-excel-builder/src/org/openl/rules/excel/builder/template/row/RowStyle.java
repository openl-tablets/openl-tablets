package org.openl.rules.excel.builder.template.row;

import org.apache.poi.ss.usermodel.CellStyle;

public interface RowStyle {

    CellStyle getTypeStyle();

    CellStyle getNameStyle();

    CellStyle getValueStyle();

}
