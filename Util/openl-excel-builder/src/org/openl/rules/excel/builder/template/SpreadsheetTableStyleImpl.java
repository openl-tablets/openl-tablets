package org.openl.rules.excel.builder.template;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.RichTextString;

import org.openl.rules.excel.builder.CellRangeSettings;
import org.openl.rules.excel.builder.template.row.NameValueRowStyle;

public class SpreadsheetTableStyleImpl extends DefaultTableStyleImpl implements SpreadsheetTableStyle {

    private final NameValueRowStyle headerRowStyle;
    private final String stepHeaderText;
    private String valueHeaderText;
    private final NameValueRowStyle rowStyle;
    private final NameValueRowStyle lastRowStyle;
    private final CellStyle dateFieldStyle;
    private final CellStyle dateTimeFieldStyle;

    public SpreadsheetTableStyleImpl(RichTextString headerTextTemplate,
                                     CellStyle headerStyle,
                                     CellRangeSettings headerSettings,
                                     NameValueRowStyle headerRowStyle,
                                     String stepHeaderText,
                                     String valueHeaderText,
                                     NameValueRowStyle rowStyle,
                                     NameValueRowStyle lastRowStyle,
                                     CellStyle dateFieldStyle,
                                     CellStyle dateTimeFieldStyle) {
        super(headerTextTemplate, headerStyle, headerSettings);
        this.headerRowStyle = headerRowStyle;
        this.stepHeaderText = stepHeaderText;
        this.valueHeaderText = valueHeaderText;
        this.rowStyle = rowStyle;
        this.lastRowStyle = lastRowStyle;
        this.dateFieldStyle = dateFieldStyle;
        this.dateTimeFieldStyle = dateTimeFieldStyle;
    }

    @Override
    public NameValueRowStyle getHeaderRowStyle() {
        return headerRowStyle;
    }

    @Override
    public String getStepHeaderText() {
        return stepHeaderText;
    }

    @Override
    public String getValueHeaderText() {
        return valueHeaderText;
    }

    @Override
    public void setValueHeaderText(String valueHeaderText) {
        this.valueHeaderText = valueHeaderText;
    }

    @Override
    public NameValueRowStyle getRowStyle() {
        return rowStyle;
    }

    @Override
    public NameValueRowStyle getLastRowStyle() {
        return lastRowStyle;
    }

    @Override
    public CellStyle getDateStyle() {
        return dateFieldStyle;
    }

    @Override
    public CellStyle getDateTimeStyle() {
        return dateTimeFieldStyle;
    }
}
