package org.openl.rules.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

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
 * Validator that checks correctness of "active" property. Only one active table allowed. And if active table is absent
 * warning will occur.
 *
 * @author PUdalau
 */
public class ActivePropertyValidator extends TablesValidator {

    public static final String NO_ACTIVE_TABLE_MESSAGE =
            "No active table for group of tables. The last version will be used for execution.";
    public static final String ODD_ACTIVE_TABLE_MESSAGE = "There can be only one active table.";

    @Override
    public ValidationResult validateTables(TableSyntaxNode[] tableSyntaxNodes, IOpenClass openClass) {
        // Group methods not TableSyntaxNodes as we may have dependent modules,
        // and no sources for them,
        // represented in current module. The only information about dependency
        // methods contains in openClass.
        //
        var groupedMethods = groupExecutableMethods(
                tableSyntaxNodes);

        var messages = new LinkedHashSet<OpenLMessage>();

        for (DimensionPropertiesMethodKey key : groupedMethods.keySet()) {
            List<TableSyntaxNode> methodsGroup = groupedMethods.get(key);
            var activeExecutableMethodTable = new ArrayList<TableSyntaxNode>();
            var activeTableFoundCount = 0;

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

    private static boolean isActive(TableSyntaxNode executableMethodTable) {
        return Boolean.TRUE.equals(executableMethodTable.getTableProperties().getActive());
    }

    private static Map<DimensionPropertiesMethodKey, List<TableSyntaxNode>> groupExecutableMethods(
            TableSyntaxNode[] tableSyntaxNodes) {
        var groupedMethods = new HashMap<DimensionPropertiesMethodKey, List<TableSyntaxNode>>();

        for (TableSyntaxNode tsn : tableSyntaxNodes) {
            if (tsn.getMember() instanceof ExecutableRulesMethod) {
                var executableMethod = (ExecutableRulesMethod) tsn.getMember();
                var key = new DimensionPropertiesMethodKey(executableMethod);
                if (!groupedMethods.containsKey(key)) {
                    groupedMethods.put(key, new ArrayList<>());
                }
                groupedMethods.get(key).add(tsn);
            }
        }
        return groupedMethods;
    }
}
