package org.openl.rules.validation;

import java.util.HashMap;
import java.util.Map;

import org.openl.OpenL;
import org.openl.message.OpenLWarnMessage;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.types.IOpenClass;
import org.openl.validation.ValidationResult;
import org.openl.validation.ValidationStatus;
import org.openl.validation.ValidationUtils;

public class UniquePropertyValueValidator extends TablesValidator {

    private String propertyName;

    public UniquePropertyValueValidator(String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    public ValidationResult validateTables(OpenL openl, TableSyntaxNode[] tableSyntaxNodes, IOpenClass openClass) {

        Map<Object, TableSyntaxNode> values = new HashMap<Object, TableSyntaxNode>();
        ValidationResult validationResult = null;

        for (TableSyntaxNode tableSyntaxNode : tableSyntaxNodes) {

            ITableProperties tableProperties = tableSyntaxNode.getTableProperties();

            if (tableProperties == null) {

                // Skip current table validation.
                //
                continue;
            }

            // Get property value.
            //
            Object value = tableProperties.getPropertyValue(propertyName);

            if (value == null) {
                continue;
            }

            // Check that table with same property value doesn't exist. If
            // table with the same property value exists then create/add
            // validation error message else add current property value to list
            // of processed values.
            //
            if (values.containsKey(value)) {

                TableSyntaxNode existsTable = values.get(value);

                // String message =
                // String.format("Found tables with duplicate property '%s': %s [%s], %s [%s]",
                // propertyName,
                // existsTable.getHeaderLineValue().getValue(),
                // existsTable.getUri(),
                // tableSyntaxNode.getHeaderLineValue().getValue(),
                // tableSyntaxNode.getUri());

                String message = String.format("Found tables with duplicate property '%s'", propertyName);

                if (validationResult == null) {
                    validationResult = new ValidationResult(ValidationStatus.FAIL, null);

                    OpenLWarnMessage warnMessage1 = new OpenLWarnMessage(message, existsTable);
                    OpenLWarnMessage warnMessage2 = new OpenLWarnMessage(message, tableSyntaxNode);

                    ValidationUtils.addValidationMessage(validationResult, warnMessage1);
                    ValidationUtils.addValidationMessage(validationResult, warnMessage2);

                } else {
                    OpenLWarnMessage warnMessage = new OpenLWarnMessage(message, tableSyntaxNode);
                    ValidationUtils.addValidationMessage(validationResult, warnMessage);
                }
            } else {
                values.put(value, tableSyntaxNode);
            }
        }

        // Return validation result if it not null (it is not null if at
        // least one error has occurred).
        //
        if (validationResult != null) {
            return validationResult;
        }

        return ValidationUtils.validationSuccess();
    }

}
