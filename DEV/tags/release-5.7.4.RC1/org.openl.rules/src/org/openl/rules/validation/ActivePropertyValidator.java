package org.openl.rules.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.OpenL;
import org.openl.message.OpenLErrorMessage;
import org.openl.message.OpenLWarnMessage;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeKey;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.validation.ValidationResult;
import org.openl.validation.ValidationStatus;
import org.openl.validation.ValidationUtils;

/**
 * Validator that checks correctness of "active" property. Only one active table
 * allowed. And if active table is absent warning will occur.
 * 
 * @author PUdalau
 */
public class ActivePropertyValidator extends TablesValidator {

    public static final String NO_ACTIVE_TABLE_MESSAGE = "No active table";
    public static final String ODD_ACTIVE_TABLE_MESSAGE = "There can be only one active table";

    @Override
    public ValidationResult validateTables(OpenL openl, TableSyntaxNode[] tableSyntaxNodes, IOpenClass openClass) {
        ValidationResult validationResult = null;
        Map<TableSyntaxNodeKey, List<TableSyntaxNode>> groupedTables = groupExecutableTables(tableSyntaxNodes);
        for (TableSyntaxNodeKey key : groupedTables.keySet()) {
            List<TableSyntaxNode> tablesGroup = groupedTables.get(key);
            boolean activeTableWasFound = false;
            for (TableSyntaxNode tsn : tablesGroup) {
                if (Boolean.TRUE.equals(tsn.getTableProperties().getActive())) {
                    if (activeTableWasFound) {
                        if (validationResult == null) {
                            validationResult = new ValidationResult(ValidationStatus.FAIL);
                        }
                        SyntaxNodeException exception = new SyntaxNodeException(ODD_ACTIVE_TABLE_MESSAGE, null, tsn);
                        tsn.addError(exception);
                        ValidationUtils.addValidationMessage(validationResult, new OpenLErrorMessage(exception));
                    } else {
                        activeTableWasFound = true;
                    }
                }
            }
            if (!activeTableWasFound) {
                if (validationResult == null) {
                    validationResult = new ValidationResult(ValidationStatus.SUCCESS);
                }
                // warning is attached to any table syntax node
                ValidationUtils.addValidationMessage(validationResult, new OpenLWarnMessage(NO_ACTIVE_TABLE_MESSAGE,
                        tablesGroup.get(0)));
            }
        }
        if (validationResult != null) {
            return validationResult;
        } else {
            return ValidationUtils.validationSuccess();
        }
    }

    private Map<TableSyntaxNodeKey, List<TableSyntaxNode>> groupExecutableTables(TableSyntaxNode[] tableSyntaxNodes) {
        Map<TableSyntaxNodeKey, List<TableSyntaxNode>> groupedTables = new HashMap<TableSyntaxNodeKey, List<TableSyntaxNode>>();
        for (TableSyntaxNode tsn : tableSyntaxNodes) {
            if (tsn.getMember() instanceof IOpenMethod) {
                TableSyntaxNodeKey key = new TableSyntaxNodeKey(tsn);
                if (!groupedTables.containsKey(key)) {
                    groupedTables.put(key, new ArrayList<TableSyntaxNode>());
                }
                groupedTables.get(key).add(tsn);
            }
        }
        return groupedTables;
    }
}
