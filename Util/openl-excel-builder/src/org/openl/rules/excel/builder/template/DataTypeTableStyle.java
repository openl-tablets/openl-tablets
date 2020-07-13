package org.openl.rules.excel.builder.template;

import org.openl.rules.excel.builder.CellRangeSettings;
import org.openl.rules.table.ui.ICellStyle;

public class DataTypeTableStyle extends DefaultTableStyle implements TableStyle {

    private ICellStyle fieldTypeStyle;
    private ICellStyle fieldNameStyle;
    private ICellStyle fieldDefaultValueStyle;

    public DataTypeTableStyle(String headerTextTemplate,
                              ICellStyle headerStyle,
                              CellRangeSettings headerSettings,
                              ICellStyle fieldTypeStyle,
                              ICellStyle fieldNameStyle,
                              ICellStyle fieldDefaultValueStyle) {
        super(headerTextTemplate, headerStyle, headerSettings);
        this.fieldTypeStyle = fieldTypeStyle;
        this.fieldNameStyle = fieldNameStyle;
        this.fieldDefaultValueStyle = fieldDefaultValueStyle;
    }

    public ICellStyle getFieldTypeStyle() {
        return fieldTypeStyle;
    }

    public void setFieldTypeStyle(ICellStyle fieldTypeStyle) {
        this.fieldTypeStyle = fieldTypeStyle;
    }

    public ICellStyle getFieldNameStyle() {
        return fieldNameStyle;
    }

    public void setFieldNameStyle(ICellStyle fieldNameStyle) {
        this.fieldNameStyle = fieldNameStyle;
    }

    public ICellStyle getFieldDefaultValueStyle() {
        return fieldDefaultValueStyle;
    }

    public void setFieldDefaultValueStyle(ICellStyle fieldDefaultValueStyle) {
        this.fieldDefaultValueStyle = fieldDefaultValueStyle;
    }

}
