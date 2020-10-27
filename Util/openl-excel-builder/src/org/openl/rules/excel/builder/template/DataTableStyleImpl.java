package org.openl.rules.excel.builder.template;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.RichTextString;
import org.openl.rules.excel.builder.CellRangeSettings;
import org.openl.rules.excel.builder.template.row.NameValueRowStyle;

public class DataTableStyleImpl extends DefaultTableStyleImpl implements DataTableStyle {

    private final Font typeFont;
    private final Font tableNameFont;
    private final CellStyle subheaderStyle;
    private final CellStyle columnHeaderStyle;
    private final NameValueRowStyle rowStyle;
    private final CellStyle dateFieldStyle;
    private final CellStyle dateTimeFieldStyle;

    public DataTableStyleImpl(RichTextString headerTextTemplate,
            CellStyle headerStyle,
            CellRangeSettings headerSizeSettings,
            Font typeFont,
            Font tableNameFont,
            CellStyle subheaderStyle,
            CellStyle columnHeaderStyle,
            NameValueRowStyle rowStyle,
            CellStyle dateFieldStyle,
            CellStyle dateTimeFieldStyle) {
        super(headerTextTemplate, headerStyle, headerSizeSettings);
        this.typeFont = typeFont;
        this.tableNameFont = tableNameFont;
        this.subheaderStyle = subheaderStyle;
        this.columnHeaderStyle = columnHeaderStyle;
        this.rowStyle = rowStyle;
        this.dateFieldStyle = dateFieldStyle;
        this.dateTimeFieldStyle = dateTimeFieldStyle;
    }

    @Override
    public Font getTypeFont() {
        return typeFont;
    }

    @Override
    public Font getTableNameFont() {
        return tableNameFont;
    }

    @Override
    public CellStyle getSubheaderStyle() {
        return subheaderStyle;
    }

    @Override
    public CellStyle getColumnHeaderStyle() {
        return columnHeaderStyle;
    }

    @Override
    public NameValueRowStyle getRowStyle() {
        return rowStyle;
    }

    @Override
    public NameValueRowStyle getLastRowStyle() {
        return rowStyle;
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
