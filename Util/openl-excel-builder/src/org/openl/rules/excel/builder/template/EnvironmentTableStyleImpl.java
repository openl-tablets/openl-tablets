package org.openl.rules.excel.builder.template;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.RichTextString;
import org.openl.rules.excel.builder.CellRangeSettings;
import org.openl.rules.excel.builder.template.row.NameValueRowStyle;

public class EnvironmentTableStyleImpl extends DefaultTableStyleImpl implements TableStyle {

    private final NameValueRowStyle rowStyle;
    private final NameValueRowStyle lastRowStyle;

    public EnvironmentTableStyleImpl(RichTextString headerTextTemplate,
            CellStyle headerStyle,
            CellRangeSettings headerSizeSettings,
            NameValueRowStyle rowStyle,
            NameValueRowStyle lastRowStyle) {
        super(headerTextTemplate, headerStyle, headerSizeSettings);
        this.rowStyle = rowStyle;
        this.lastRowStyle = lastRowStyle;
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
        return getHeaderStyle();
    }

    @Override
    public CellStyle getDateTimeStyle() {
        return getHeaderStyle();
    }
}
