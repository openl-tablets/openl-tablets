package org.openl.rules.excel.builder.template;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.RichTextString;

import org.openl.rules.excel.builder.CellRangeSettings;

@RequiredArgsConstructor
public abstract class DefaultTableStyleImpl implements TableStyle {

    private final RichTextString headerTextTemplate;
    @Getter
    private final CellStyle headerStyle;
    @Getter
    private final CellRangeSettings headerSizeSettings;

    @Override
    public RichTextString getHeaderTemplate() {
        return headerTextTemplate;
    }
}
