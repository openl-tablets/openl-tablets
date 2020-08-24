package org.openl.rules.excel.builder.template;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.RichTextString;
import org.openl.rules.excel.builder.CellRangeSettings;
import org.openl.rules.excel.builder.template.row.DataTypeRowStyle;

public class DataTypeTableStyleImpl extends DefaultTableStyleImpl implements DataTypeTableStyle {

    private final DataTypeRowStyle dataTypeRowStyle;
    private final CellStyle dateFieldStyle;
    private final DataTypeRowStyle lastRowStyle;
    private final Font datatypeFont;

    public DataTypeTableStyleImpl(RichTextString headerTextTemplate,
            CellStyle headerStyle,
            CellRangeSettings headerSizeSettings,
            DataTypeRowStyle rowStyle,
            CellStyle dateFieldStyle,
            DataTypeRowStyle lastRowStyle,
            Font datatypeFont) {
        super(headerTextTemplate, headerStyle, headerSizeSettings);
        this.dataTypeRowStyle = rowStyle;
        this.dateFieldStyle = dateFieldStyle;
        this.lastRowStyle = lastRowStyle;
        this.datatypeFont = datatypeFont;
    }

    @Override
    public DataTypeRowStyle getRowStyle() {
        return dataTypeRowStyle;
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
    public DataTypeRowStyle getLastRowStyle() {
        return lastRowStyle;
    }
}
