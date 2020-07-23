package org.openl.rules.excel.builder.template;

import org.apache.poi.ss.usermodel.CellStyle;
import org.openl.rules.excel.builder.CellRangeSettings;
import org.openl.rules.excel.builder.template.row.DataTypeTableRowStyle;

public class DataTypeTableStyle extends DefaultTableStyle implements TableStyle {

    private DataTypeTableRowStyle rowStyle;
    private CellStyle fieldDateStyle;
    private DataTypeTableRowStyle lastRowStyle;

    public DataTypeTableStyle(String headerTextTemplate,
            CellStyle headerStyle,
            CellRangeSettings headerSettings,
            DataTypeTableRowStyle rowStyle,
            CellStyle fieldDateStyle,
            DataTypeTableRowStyle lastRowStyle) {
        super(headerTextTemplate, headerStyle, headerSettings);
        this.rowStyle = rowStyle;
        this.fieldDateStyle = fieldDateStyle;
        this.lastRowStyle = lastRowStyle;
    }

    public DataTypeTableRowStyle getRowStyle() {
        return rowStyle;
    }

    public CellStyle getFieldDateStyle() {
        return fieldDateStyle;
    }

    public DataTypeTableRowStyle getLastRowStyle() {
        return lastRowStyle;
    }
}
