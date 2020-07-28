package org.openl.rules.excel.builder.template;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.RichTextString;
import org.openl.rules.excel.builder.CellRangeSettings;
import org.openl.rules.excel.builder.template.row.DataTypeTableRowStyle;
import org.openl.rules.excel.builder.template.row.RowStyle;

public class DataTypeTableStyleImpl extends DefaultTableStyleImpl implements DataTypeTableStyle {

    private RowStyle rowStyle;
    private CellStyle dateFieldStyle;
    private DataTypeTableRowStyle lastRowStyle;
    private Font datatypeFont;

    public DataTypeTableStyleImpl(RichTextString headerTextTemplate,
            CellStyle headerStyle,
            CellRangeSettings headerSizeSettings,
            DataTypeTableRowStyle rowStyle,
            CellStyle dateFieldStyle,
            DataTypeTableRowStyle lastRowStyle,
            Font datatypeFont) {
        super(headerTextTemplate, headerStyle, headerSizeSettings);
        this.rowStyle = rowStyle;
        this.dateFieldStyle = dateFieldStyle;
        this.lastRowStyle = lastRowStyle;
        this.datatypeFont = datatypeFont;
    }

    @Override
    public RowStyle getRowStyle() {
        return rowStyle;
    }

    @Override
    public CellStyle getDateFieldStyle() {
        return dateFieldStyle;
    }

    @Override
    public Font getHeaderFont() {
        return datatypeFont;
    }

    @Override
    public RowStyle getLastRowStyle() {
        return lastRowStyle;
    }
}
