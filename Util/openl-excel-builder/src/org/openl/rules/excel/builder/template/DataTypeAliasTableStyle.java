package org.openl.rules.excel.builder.template;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.RichTextString;
import org.openl.rules.excel.builder.CellRangeSettings;
import org.openl.rules.excel.builder.template.row.RowStyle;

public class DataTypeAliasTableStyle extends DefaultTableStyleImpl implements DataTypeTableStyle {

    private CellStyle fieldStyle;
    private RowStyle rowStyle;
    private RowStyle lastRowStyle;

    public DataTypeAliasTableStyle(RichTextString headerTextTemplate,
            CellStyle headerStyle,
            CellRangeSettings headerSizeSettings,
            CellStyle fieldStyle,
            RowStyle rowStyle,
            RowStyle lastRowStyle) {
        super(headerTextTemplate, headerStyle, headerSizeSettings);
        this.fieldStyle = fieldStyle;
        this.rowStyle = rowStyle;
        this.lastRowStyle = lastRowStyle;
    }

    public CellStyle getFieldStyle() {
        return fieldStyle;
    }

    @Override
    public CellStyle getDateFieldStyle() {
        return fieldStyle;
    }

    @Override
    public Font getHeaderFont() {
        return null;
    }

    @Override
    public RowStyle getRowStyle() {
        return rowStyle;
    }

    @Override
    public RowStyle getLastRowStyle() {
        return lastRowStyle;
    }
}
