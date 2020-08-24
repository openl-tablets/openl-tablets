package org.openl.rules.excel.builder.template;

import org.openl.rules.excel.builder.template.row.NameValueRowStyle;

public interface SpreadsheetTableStyle extends TableStyle {

    String getValueHeaderText();

    String getStepHeaderText();

    NameValueRowStyle getHeaderRowStyle();

}
