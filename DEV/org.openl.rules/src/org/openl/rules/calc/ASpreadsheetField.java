package org.openl.rules.calc;

import lombok.Getter;

import org.openl.types.IOpenClass;
import org.openl.types.impl.DynamicObjectField;
import org.openl.util.StringPool;

public abstract class ASpreadsheetField extends DynamicObjectField {

    @Getter
    private String columnName;
    @Getter
    private String rowName;

    public ASpreadsheetField(IOpenClass declaringClass, String name, IOpenClass type) {
        super(declaringClass, name, type);
    }

    public ASpreadsheetField(IOpenClass declaringClass, String columnName, String rowName, IOpenClass type) {
        super(declaringClass, createFieldName(columnName, rowName), type);
        this.columnName = columnName;
        this.rowName = rowName;
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
