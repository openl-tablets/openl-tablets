package org.openl.rules.excel.builder.template;

import lombok.Getter;
import lombok.Setter;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.RichTextString;

import org.openl.rules.excel.builder.CellRangeSettings;
import org.openl.rules.excel.builder.template.row.NameValueRowStyle;

public class SpreadsheetTableStyleImpl extends DefaultTableStyleImpl implements SpreadsheetTableStyle {

    @Getter
    private final NameValueRowStyle headerRowStyle;
    @Getter
    private final String stepHeaderText;
    @Getter
    @Setter
    private String valueHeaderText;
    @Getter
    private final NameValueRowStyle rowStyle;
    @Getter
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
    public CellStyle getDateStyle() {
        return dateFieldStyle;
    }

    @Override
    public CellStyle getDateTimeStyle() {
        return dateTimeFieldStyle;
    }
}
