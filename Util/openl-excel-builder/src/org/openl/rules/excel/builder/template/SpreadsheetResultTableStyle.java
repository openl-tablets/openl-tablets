package org.openl.rules.excel.builder.template;

import org.apache.poi.ss.usermodel.CellStyle;
import org.openl.rules.excel.builder.CellRangeSettings;
import org.openl.rules.excel.builder.template.row.SpreadsheetTableRowStyle;

public class SpreadsheetResultTableStyle extends DefaultTableStyle implements TableStyle {

    private SpreadsheetTableRowStyle headerRowStyle;
    private String stepHeaderText;
    private String valueHeaderText;
    private SpreadsheetTableRowStyle rowStyle;
    private SpreadsheetTableRowStyle lastRowStyle;

    public SpreadsheetResultTableStyle(String headerTextTemplate,
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

    public SpreadsheetTableRowStyle getHeaderRowStyle() {
        return headerRowStyle;
    }

    public String getStepHeaderText() {
        return stepHeaderText;
    }

    public String getValueHeaderText() {
        return valueHeaderText;
    }

    public SpreadsheetTableRowStyle getRowStyle() {
        return rowStyle;
    }

    public SpreadsheetTableRowStyle getLastRowStyle() {
        return lastRowStyle;
    }
}
