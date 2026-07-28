package org.openl.rules.excel.builder.template;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.RichTextString;

import org.openl.rules.excel.builder.CellRangeSettings;

@RequiredArgsConstructor
public abstract class DefaultTableStyleImpl implements TableStyle {

    private final RichTextString headerTextTemplate;
    private final CellStyle headerStyle;
    private final CellRangeSettings headerSizeSettings;

    @Override
    public CellStyle getHeaderStyle() {
        return headerStyle;
    }

    @Override
    public CellRangeSettings getHeaderSizeSettings() {
        return headerSizeSettings;
    }

    @Override
    public RichTextString getHeaderTemplate() {
        return headerTextTemplate;
    }
}
