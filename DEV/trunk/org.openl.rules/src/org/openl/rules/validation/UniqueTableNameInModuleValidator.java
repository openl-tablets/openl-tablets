package org.openl.rules.validation;

import java.util.HashMap;
import java.util.Map;

import org.openl.OpenL;
import org.openl.message.Severity;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.types.IOpenClass;
import org.openl.validation.IOpenLValidator;
import org.openl.validation.ValidationResult;
import org.openl.validation.ValidationUtils;

/**
 * Checks that names of table syntax nodes have different names in xls module.
 * 
 */
public class UniqueTableNameInModuleValidator implements IOpenLValidator {

    /**
     * {@inheritDoc}
     */
    public ValidationResult validate(OpenL openl, IOpenClass openClass) {

        if (openClass instanceof XlsModuleOpenClass) {

            // Get all table syntax nodes of xls module.
            //
            XlsMetaInfo xlsMetaInfo = ((XlsModuleOpenClass) openClass).getXlsMetaInfo();
            TableSyntaxNode[] tableSyntaxNodes = xlsMetaInfo.getXlsModuleNode().getXlsTableSyntaxNodes();

            ValidationResult validationResult = null;

            Map<String, TableSyntaxNode> tables = new HashMap<String, TableSyntaxNode>();

            for (TableSyntaxNode tableSyntaxNode : tableSyntaxNodes) {

                ITableProperties tableProperties = tableSyntaxNode.getTableProperties();

                // Ignore tables what have not table properties section and have
                // not defined name property.
                //
                if (tableProperties != null && tableProperties.getName() != null) {

                    // Get table name.
                    //
                    String name = tableProperties.getName();

                    // Check that table with same name doesn't exist. If
                    // table with the name exists then create/add validation
                    // error message else add current name to list of processed
                    // names.
                    //
                    if (tables.containsKey(name)) {

                        TableSyntaxNode existedTable = tables.get(name);

                        String message = String.format("Found tables with duplicate name '%s': %s [%s], %s [%s]", name,
                                existedTable.getHeaderLineValue().getValue(), existedTable.getUri(), tableSyntaxNode
                                        .getHeaderLineValue().getValue(), tableSyntaxNode.getUri());

                        if (validationResult == null) {
                            validationResult = ValidationUtils.validationError(message);
                        } else {
                            ValidationUtils.addValidationMessage(validationResult, message, Severity.ERROR);
                        }
                    } else {

                        tables.put(name, tableSyntaxNode);
                    }

                }
            }

            // Return validation result if it not null (it is not null if at
            // least one error has occurred).
            //
            if (validationResult != null) {
                return validationResult;
            }
        }

        // Skip validation if passed open class is not instance of
        // XlsModuleOpenClass.
        //
        return ValidationUtils.validationSuccess();
    }

}
