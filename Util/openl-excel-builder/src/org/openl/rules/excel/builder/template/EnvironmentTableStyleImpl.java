package org.openl.rules.excel.builder.template;

import lombok.Getter;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.RichTextString;

import org.openl.rules.excel.builder.CellRangeSettings;
import org.openl.rules.excel.builder.template.row.NameValueRowStyle;

public class EnvironmentTableStyleImpl extends DefaultTableStyleImpl implements TableStyle {

    @Getter
    private final NameValueRowStyle rowStyle;
    @Getter
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
    public CellStyle getDateStyle() {
        return getHeaderStyle();
    }

    @Override
    public CellStyle getDateTimeStyle() {
        return getHeaderStyle();
    }
}
