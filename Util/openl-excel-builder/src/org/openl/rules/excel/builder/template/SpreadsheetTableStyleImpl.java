package org.openl.rules.excel.builder.template;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.RichTextString;
import org.openl.rules.excel.builder.CellRangeSettings;
import org.openl.rules.excel.builder.template.row.RowStyle;
import org.openl.rules.excel.builder.template.row.SpreadsheetTableRowStyle;

public class SpreadsheetTableStyleImpl extends DefaultTableStyleImpl implements SpreadsheetTableStyle {

    private RowStyle headerRowStyle;
    private String stepHeaderText;
    private String valueHeaderText;
    private SpreadsheetTableRowStyle rowStyle;
    private SpreadsheetTableRowStyle lastRowStyle;

    public SpreadsheetTableStyleImpl(RichTextString headerTextTemplate,
            CellStyle headerStyle,
            CellRangeSettings headerSettings,
            SpreadsheetTableRowStyle headerRowStyle,
            String stepHeaderText,
            String valueHeaderText,
            SpreadsheetTableRowStyle rowStyle,
            SpreadsheetTableRowStyle lastRowStyle) {
        super(headerTextTemplate, headerStyle, headerSettings);
        this.headerRowStyle = headerRowStyle;
        this.stepHeaderText = stepHeaderText;
        this.valueHeaderText = valueHeaderText;
        this.rowStyle = rowStyle;
        this.lastRowStyle = lastRowStyle;
    }

    public RowStyle getHeaderRowStyle() {
        return headerRowStyle;
    }

    public String getStepHeaderText() {
        return stepHeaderText;
    }

    public String getValueHeaderText() {
        return valueHeaderText;
    }

    public RowStyle getRowStyle() {
        return rowStyle;
    }

    public SpreadsheetTableRowStyle getLastRowStyle() {
        return lastRowStyle;
    }
}
