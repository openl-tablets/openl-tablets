package org.openl.rules.excel.builder.template;

import org.openl.rules.excel.builder.CellRangeSettings;
import org.openl.rules.table.ui.ICellStyle;

public abstract class DefaultTableStyle implements TableStyle {

    private String headerTextTemplate;
    private ICellStyle headerStyle;
    private CellRangeSettings headerSettings;

    public DefaultTableStyle(String headerTextTemplate, ICellStyle headerStyle, CellRangeSettings headerSettings) {
        this.headerTextTemplate = headerTextTemplate;
        this.headerStyle = headerStyle;
        this.headerSettings = headerSettings;
    }

    public void setHeaderTextTemplate(String headerTextTemplate) {
        this.headerTextTemplate = headerTextTemplate;
    }

    public ICellStyle getHeaderStyle() {
        return headerStyle;
    }

    public void setHeaderStyle(ICellStyle headerStyle) {
        this.headerStyle = headerStyle;
    }

    public CellRangeSettings getHeaderSettings() {
        return headerSettings;
    }

    public void setHeaderSettings(CellRangeSettings headerSettings) {
        this.headerSettings = headerSettings;
    }

    @Override
    public String getHeaderTemplate() {
        return headerTextTemplate;
    }
}
