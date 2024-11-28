package org.openl.rules.calc;

import org.openl.types.IOpenClass;
import org.openl.types.impl.DynamicObjectField;
import org.openl.util.StringPool;

public abstract class ASpreadsheetField extends DynamicObjectField {

    private String columnName;
    private String rowName;

    public ASpreadsheetField(IOpenClass declaringClass, String name, IOpenClass type) {
        super(declaringClass, name, type);
    }

    public ASpreadsheetField(IOpenClass declaringClass, String columnName, String rowName, IOpenClass type) {
        super(declaringClass, createFieldName(columnName, rowName), type);
        this.columnName = columnName;
        this.rowName = rowName;
    }

    public String getColumnName() {
        return columnName;
    }

    public String getRowName() {
        return rowName;
    }

    public static String createFieldName(String columnName, String rowName) {
        var builder = new StringBuilder(64);
        if (columnName != null) {
            builder.append(SpreadsheetStructureBuilder.DOLLAR_SIGN).append(columnName);
        }
        if (rowName != null) {
            builder.append(SpreadsheetStructureBuilder.DOLLAR_SIGN).append(rowName);
        }
        return StringPool.intern(builder.toString());
    }
}
