package org.openl.util;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

public class MessageUtils {

    public static final String EMPTY_UNQ_IDX_KEY = "Empty key in an unique index.";

    private MessageUtils() {
    }

    public static String getTypeNotFoundMessage(String typeName) {
        return "Type '%s' is not found.".formatted(typeName);
    }

    public static String getConstructorNotFoundMessage(String constructorSignature) {
        return "Constructor '%s' is not found.".formatted(constructorSignature);
    }

    public static String getTypeDefinedErrorMessage(String typeName) {
        return "Type '%s' is defined with errors.".formatted(typeName);
    }

    public static String getDuplicatedKeyIndexErrorMessage(String source) {
        return "Duplicated key in an unique index: %s".formatted(source);
    }

    public static String getColumnNotFoundErrorMessage(String columnName) {
        return "Column '%s' is not found.".formatted(columnName);
    }

    public static String getUnknownForeignKeyIndexErrorMessage(String source, String fkTableName) {
        return "Index Key '%s' is not found in the foreign table '%s'.".formatted(source, fkTableName);
    }

    public static String getIncompatibleTypesErrorMessage(IOpenField fieldName, IOpenClass type, IOpenClass resType) {
        return "Field '%s' type is '%s' that is incompatible with type '%s'.".formatted(
                fieldName,
                type.getName(),
                resType.getName());
    }

    public static String getTableNotFoundErrorMessage(String tableName) {
        return "Table '%s' is not found.".formatted(tableName);
    }

    public static String getForeignTableCompilationErrorsMessage(String foreignKeyTableName) {
        return "Foreign table '%s' has errors.".formatted(foreignKeyTableName);
    }

    public static String getConditionMultipleExpressionErrorMessage(String conditionName) {
        return "Multiple expressions are defined for '%s' condition.".formatted(conditionName);
    }

}
