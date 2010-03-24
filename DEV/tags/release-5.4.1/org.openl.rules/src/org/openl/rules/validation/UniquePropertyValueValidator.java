package org.openl.rules.validation;

import java.util.HashMap;
import java.util.Map;

import org.openl.OpenL;
import org.openl.message.Severity;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.validation.ValidationResult;
import org.openl.validation.ValidationUtils;

public class UniquePropertyValueValidator extends TablesValidator {

    private String propertyName;

    public UniquePropertyValueValidator(String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    public ValidationResult validateTables(OpenL openl, TableSyntaxNode[] tableSyntaxNodes) {

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

                String message = String.format("Found tables with duplicate property '%s': %s [%s], %s [%s]",
                        propertyName, existsTable.getHeaderLineValue().getValue(), existsTable.getUri(),
                        tableSyntaxNode.getHeaderLineValue().getValue(), tableSyntaxNode.getUri());

                if (validationResult == null) {
                    validationResult = ValidationUtils.validationError(message);
                } else {
                    ValidationUtils.addValidationMessage(validationResult, message, Severity.ERROR);
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
