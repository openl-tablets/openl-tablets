package org.openl.rules.excel.builder.template;

import org.openl.rules.excel.builder.template.row.RowStyle;

public interface SpreadsheetTableStyle extends TableStyle {

    String getValueHeaderText();

    String getStepHeaderText();

    RowStyle getHeaderRowStyle();

}
