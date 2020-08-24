package org.openl.rules.excel.builder.template;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.RichTextString;
import org.openl.rules.excel.builder.CellRangeSettings;
import org.openl.rules.excel.builder.template.row.NameValueRowStyle;

public class SpreadsheetTableStyleImpl extends DefaultTableStyleImpl implements SpreadsheetTableStyle {

    private final NameValueRowStyle headerRowStyle;
    private final String stepHeaderText;
    private final String valueHeaderText;
    private final NameValueRowStyle rowStyle;
    private final NameValueRowStyle lastRowStyle;

    public SpreadsheetTableStyleImpl(RichTextString headerTextTemplate,
            CellStyle headerStyle,
            CellRangeSettings headerSettings,
            NameValueRowStyle headerRowStyle,
            String stepHeaderText,
            String valueHeaderText,
            NameValueRowStyle rowStyle,
            NameValueRowStyle lastRowStyle) {
        super(headerTextTemplate, headerStyle, headerSettings);
        this.headerRowStyle = headerRowStyle;
        this.stepHeaderText = stepHeaderText;
        this.valueHeaderText = valueHeaderText;
        this.rowStyle = rowStyle;
        this.lastRowStyle = lastRowStyle;
    }

    public NameValueRowStyle getHeaderRowStyle() {
        return headerRowStyle;
    }

    public String getStepHeaderText() {
        return stepHeaderText;
    }

    public String getValueHeaderText() {
        return valueHeaderText;
    }

    public NameValueRowStyle getRowStyle() {
        return rowStyle;
    }

    public NameValueRowStyle getLastRowStyle() {
        return lastRowStyle;
    }
}
