package org.openl.rules.table.properties.inherit;

import java.util.Arrays;
import java.util.Set;

import org.openl.binding.IBindingContext;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.def.DefaultPropertyDefinitions;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to check properties according to some situations.
 *
 * @author DLiauchuk
 */
public final class PropertiesChecker {

    private PropertiesChecker() {
    }

    /**
     * We need to check loaded properties that all values are appropriate for this table. If there is any problem an
     * error should be thrown. Now we check 3 situations:<br>
     * 1) properties can be defined on TABLE level;<br>
     * 2) properties can be defined for current type of table; 3) deprecated properties;
     *
     * @param propertyNamesToCheck properties names that are physically defined in table. TODO: Refactor with strategy
     *            pattern
     */
    public static void checkProperties(IBindingContext bindingContext,
            Set<String> propertyNamesToCheck,
            TableSyntaxNode tableSyntaxNode,
            InheritanceLevel level) {
        checkForErrors(bindingContext, propertyNamesToCheck, tableSyntaxNode, level);
        checkForDeprecation(bindingContext, propertyNamesToCheck, tableSyntaxNode);
    }

    /**
     * Checks if properties can be defined for given type of table and for given inheritance level.
     */
    private static void checkForErrors(IBindingContext bindingContext,
            Set<String> propertyNamesToCheck,
            TableSyntaxNode tableSyntaxNode,
            InheritanceLevel level) {
        String tableType = tableSyntaxNode.getType();
        String typeName = getTypeName(tableSyntaxNode);

        for (String propertyNameToCheck : propertyNamesToCheck) {
            if (!PropertiesChecker.isPropertySuitableForTableType(propertyNameToCheck, tableType)) {
                String message = String
                    .format("Property '%s' cannot be defined in %s Table", propertyNameToCheck, typeName);

                addError(bindingContext, tableSyntaxNode, message);
            } else if (level != null && !PropertiesChecker.isPropertySuitableForLevel(level, propertyNameToCheck)) {
                String message = String.format("Property '%s' cannot be defined on the '%s' level",
                    propertyNameToCheck,
                    level.getDisplayName());

                addError(bindingContext, tableSyntaxNode, message);
            }
        }
    }

    private static void addError(IBindingContext bindingContext, TableSyntaxNode tableSyntaxNode, String message) {
        SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, tableSyntaxNode);
        tableSyntaxNode.addError(error);
        bindingContext.addError(error);
    }

    /**
     * Checks if properties were deprecated.
     */
    private static void checkForDeprecation(IBindingContext bindingContext,
            Set<String> propertyNamesToCheck,
            TableSyntaxNode tableSyntaxNode) {
        for (String propertyNameToCheck : propertyNamesToCheck) {
            TablePropertyDefinition propertyDefinition = TablePropertyDefinitionUtils
                .getPropertyByName(propertyNameToCheck);
            if (propertyDefinition != null && propertyDefinition.getDeprecation() != null && !propertyDefinition
                .getDeprecation()
                .isEmpty()) {
                String message = String.format("Property '%s' was deprecated. Please remove it!", propertyNameToCheck);
                bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage(message, tableSyntaxNode));
            }
        }
    }

    /**
     * Checks if property with given name is suitable for given level. Checks according to the property definitions in
     * {@link DefaultPropertyDefinitions}.
     *
     * @param currentLevel current level of current property.
     * @param propertyName name of the property to check.
     * @return true if property with income name can be defined in income level.
     */
    public static boolean isPropertySuitableForLevel(InheritanceLevel currentLevel, String propertyName) {
        final Logger log = LoggerFactory.getLogger(PropertiesChecker.class);
        boolean result = false;
        TablePropertyDefinition propertyDefinition = TablePropertyDefinitionUtils.getPropertyByName(propertyName);
        if (propertyDefinition != null) {
            InheritanceLevel[] inheritanceLevels = propertyDefinition.getInheritanceLevel();
            if (inheritanceLevels != null && inheritanceLevels.length > 0) {
                if (Arrays.asList(inheritanceLevels).contains(currentLevel)) {
                    result = true;
                }
            } else {
                log.debug("Inheritance levels were not defined for property with name [{}].", propertyName);
            }
        } else {
            log.debug("There is no such property in Definitions with name [{}].", propertyName);
        }
        return result;
    }

    /**
     * Checks if properties can be defined for given type of table.
     *
     * @return TRUE if given property can be set for given type of table.
     */
    public static boolean isPropertySuitableForTableType(String propertyName, String tableType) {
        XlsNodeTypes[] definitionTableTypes = TablePropertyDefinitionUtils.getSuitableTableTypes(propertyName);
        if (definitionTableTypes != null && definitionTableTypes.length > 0) {
            for (XlsNodeTypes nodeType : definitionTableTypes) {
                if (nodeType.toString().equals(tableType)) {
                    // If type from property definition and current table type are equals. It means property is suitable
                    // for this kind of table.
                    return true;
                }
            }
        } else {
            // If definitionTableTypes is empty, it means that property is suitable for all kinds of tables.
            return true;
        }
        return false;
    }

    private static String getTypeName(TableSyntaxNode tableSyntaxNode) {
        switch (tableSyntaxNode.getNodeType()) {
            case XLS_DT:
                return "Decision";
            case XLS_SPREADSHEET:
                return "Spreadsheet";
            case XLS_TBASIC:
                return "TBasic";
            case XLS_COLUMN_MATCH:
                return "Column Match";
            case XLS_METHOD:
                return "Method";
            case XLS_TEST_METHOD:
                return "Test";
            case XLS_RUN_METHOD:
                return "Run";
            case XLS_DATA:
                return "Data";
            case XLS_DATATYPE:
                return "Datatype";
            case XLS_OPENL:
                return "OpenL";
            case XLS_ENVIRONMENT:
                return "Environment";
            case XLS_CONSTANTS:
                return "Constants";
            case XLS_TABLEPART:
                return "TablePart";
            case XLS_OTHER:
                return "Other";
            case XLS_PROPERTIES:
                return "Properties";
            default:
                return tableSyntaxNode.getNodeType().name();
        }
    }
}
