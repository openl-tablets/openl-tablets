package org.openl.util;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

public class MessageUtils {

    public static final String EMPTY_UNQ_IDX_KEY = "Empty key in an unique index.";

    public static String getTypeNotFoundMessage(String typeName) {
        return String.format("Type '%s' is not found.", typeName);
    }

    public static String getConstructorNotFoundMessage(String constructorSignature) {
        return String.format("Constructor '%s' is not found.", constructorSignature);
    }

    public static String getTypeDefinedErrorMessage(String typeName) {
        return String.format("Type '%s' is defined with errors.", typeName);
    }

    public static String getDuplicatedKeyIndexErrorMessage(String source) {
        return String.format("Duplicated key in an unique index: %s", source);
    }

    public static String getColumnNotFoundErrorMessage(String columnName) {
        return String.format("Column '%s' is not found.", columnName);
    }

    public static String getUnknownForeignKeyIndexErrorMessage(String source, String fkTableName) {
        return String.format("Index Key '%s' is not found in the foreign table '%s'.", source, fkTableName);
    }

    public static String getIncompatibleTypesErrorMessage(IOpenField fieldName, IOpenClass type, IOpenClass resType) {
        return String.format("Incompatible types: Field '%s' type is '%s' that differs from type of foreign table '%s'.",
                fieldName,
                type,
                resType);
    }

    public static String getTableNotFoundErrorMessage(String tableName) {
        return String.format("Table '%s' is not found.", tableName);
    }

    public static String getForeignTableCompilationErrorsMessage(String foreignKeyTableName) {
        return String.format("Foreign table '%s' has errors.", foreignKeyTableName);
    }

}
