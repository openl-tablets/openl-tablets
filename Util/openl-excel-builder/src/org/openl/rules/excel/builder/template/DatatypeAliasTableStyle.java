package org.openl.rules.excel.builder.template;

import org.apache.poi.ss.usermodel.CellStyle;
import org.openl.rules.excel.builder.CellRangeSettings;

public class DataTypeAliasTableStyle extends DefaultTableStyle implements TableStyle {

    private CellStyle fieldStyle;

    public DataTypeAliasTableStyle(String headerTextTemplate,
            CellStyle headerStyle,
            CellRangeSettings headerSettings,
            CellStyle fieldStyle) {
        super(headerTextTemplate, headerStyle, headerSettings);
        this.fieldStyle = fieldStyle;
    }

    public CellStyle getFieldStyle() {
        return fieldStyle;
    }

    public void setFieldStyle(CellStyle fieldStyle) {
        this.fieldStyle = fieldStyle;
    }
}
