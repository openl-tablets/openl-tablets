package org.openl.rules.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.openl.OpenL;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.properties.DimensionPropertiesMethodKey;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenClass;
import org.openl.validation.ValidationResult;

/**
 * Validator that checks correctness of "active" property. Only one active table
 * allowed. And if active table is absent warning will occur.
 * 
 * @author PUdalau
 */
public class ActivePropertyValidator extends TablesValidator {

    public static final String NO_ACTIVE_TABLE_MESSAGE = "No active table for group of tables. The last version will be used for execution.";
    public static final String ODD_ACTIVE_TABLE_MESSAGE = "There can be only one active table.";

    @Override
    public ValidationResult validateTables(OpenL openl, TableSyntaxNode[] tableSyntaxNodes, IOpenClass openClass) {
        // Group methods not TableSyntaxNodes as we may have dependent modules,
        // and no sources for them,
        // represented in current module. The only information about dependency
        // methods contains in openClass.
        //
        Map<DimensionPropertiesMethodKey, List<TableSyntaxNode>> groupedMethods = groupExecutableMethods(
            tableSyntaxNodes);
        
        Collection<OpenLMessage> messages = new LinkedHashSet<>();

        for (DimensionPropertiesMethodKey key : groupedMethods.keySet()) {
            List<TableSyntaxNode> methodsGroup = groupedMethods.get(key);
            List<TableSyntaxNode> activeExecutableMethodTable = new ArrayList<>();
            int activeTableFoundCount = 0;

            for (TableSyntaxNode executableMethodTable : methodsGroup) {
                if (executableMethodTable.getMember() instanceof TestSuiteMethod) {
                    activeTableFoundCount++;
                    break;
                }
                if (executableMethodTable.getTableProperties() != null && isActive(executableMethodTable)) {
                    activeExecutableMethodTable.add(executableMethodTable);
                    activeTableFoundCount++;
                }
            }

            if (activeTableFoundCount > 1) {
                for (TableSyntaxNode executableMethodTable : activeExecutableMethodTable) {
                    SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(ODD_ACTIVE_TABLE_MESSAGE,
                        executableMethodTable);
                    if (openClass.equals(executableMethodTable.getMember().getDeclaringClass())) {
                        executableMethodTable.addError(error);
                    }
                    messages.add(OpenLMessagesUtils.newErrorMessage(error));
                }
            }

            if (activeTableFoundCount == 0) {
                for (TableSyntaxNode tsn : methodsGroup) {
                    messages.add(OpenLMessagesUtils.newWarnMessage(NO_ACTIVE_TABLE_MESSAGE, tsn));
                }
            }
        }

        return ValidationUtils.withMessages(messages);
    }

    private boolean isActive(TableSyntaxNode executableMethodTable) {
        return Boolean.TRUE.equals(executableMethodTable.getTableProperties().getActive());
    }

    private Map<DimensionPropertiesMethodKey, List<TableSyntaxNode>> groupExecutableMethods(
            TableSyntaxNode[] tableSyntaxNodes) {
        Map<DimensionPropertiesMethodKey, List<TableSyntaxNode>> groupedMethods = new HashMap<>();

        for (TableSyntaxNode tsn : tableSyntaxNodes) {
            if (tsn.getMember() instanceof ExecutableRulesMethod) {
                ExecutableRulesMethod executableMethod = (ExecutableRulesMethod) tsn.getMember();
                DimensionPropertiesMethodKey key = new DimensionPropertiesMethodKey(executableMethod);
                if (!groupedMethods.containsKey(key)) {
                    groupedMethods.put(key, new ArrayList<TableSyntaxNode>());
                }
                groupedMethods.get(key).add(tsn);
            }
        }
        return groupedMethods;
    }
}
