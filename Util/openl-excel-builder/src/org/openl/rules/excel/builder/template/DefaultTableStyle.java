package org.openl.rules.excel.builder.template;

import org.apache.poi.ss.usermodel.CellStyle;
import org.openl.rules.excel.builder.CellRangeSettings;

public abstract class DefaultTableStyle implements TableStyle {

    private String headerTextTemplate;
    private CellStyle headerStyle;
    private CellRangeSettings headerSettings;

    public DefaultTableStyle(String headerTextTemplate,
            CellStyle headerStyle,
            CellRangeSettings headerSettings) {
        this.headerTextTemplate = headerTextTemplate;
        this.headerStyle = headerStyle;
        this.headerSettings = headerSettings;
    }

    public CellStyle getHeaderStyle() {
        return headerStyle;
    }

    public CellRangeSettings getHeaderSettings() {
        return headerSettings;
    }

    @Override
    public String getHeaderTemplate() {
        return headerTextTemplate;
    }
}
