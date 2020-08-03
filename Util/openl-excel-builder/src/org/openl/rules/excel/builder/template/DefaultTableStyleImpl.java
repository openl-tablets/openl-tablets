package org.openl.rules.excel.builder.template;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.RichTextString;
import org.openl.rules.excel.builder.CellRangeSettings;

public abstract class DefaultTableStyleImpl implements TableStyle {

    private RichTextString headerTextTemplate;
    private CellStyle headerStyle;
    private CellRangeSettings headerSizeSettings;

    public DefaultTableStyleImpl(RichTextString headerTextTemplate,
            CellStyle headerStyle,
            CellRangeSettings headerSizeSettings) {
        this.headerTextTemplate = headerTextTemplate;
        this.headerStyle = headerStyle;
        this.headerSizeSettings = headerSizeSettings;
    }

    public CellStyle getHeaderStyle() {
        return headerStyle;
    }

    public CellRangeSettings getHeaderSizeSettings() {
        return headerSizeSettings;
    }

    @Override
    public RichTextString getHeaderTemplate() {
        return headerTextTemplate;
    }
}
