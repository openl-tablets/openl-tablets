package org.openl.rules.validation;

import java.util.Collection;
import java.util.LinkedHashSet;

import org.openl.OpenL;
import org.openl.message.OpenLErrorMessage;
import org.openl.message.OpenLMessage;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.constraints.RegexpValueConstraint;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.rules.table.properties.inherit.PropertiesChecker;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenClass;
import org.openl.validation.ValidationResult;

/**
 * Validator for string properties that have to correspond to some regexp pattern
 *
 * @author PUdalau
 */
public class RegexpPropertyValidator extends TablesValidator {
    private String propertyName;
    private String constraintsStr;

    public RegexpPropertyValidator(String propertyName, String constraintsStr) {
        this.propertyName = propertyName;
        this.constraintsStr = RegexpValueConstraint.getRegexPattern(constraintsStr);
    }

    @Override
    public ValidationResult validateTables(OpenL openl, TableSyntaxNode[] tableSyntaxNodes, IOpenClass openClass) {
        Collection<OpenLMessage> messages = new LinkedHashSet<>();
        for (TableSyntaxNode tsn : tableSyntaxNodes) {
            if (PropertiesChecker.isPropertySuitableForTableType(propertyName,
                tsn.getType()) && tsn.getTableProperties() != null && tsn.getTableProperties()
                    .getPropertyLevelDefinedOn(propertyName) == InheritanceLevel.TABLE) {
                String propertyValue = (String) tsn.getTableProperties().getPropertyValue(propertyName);
                if (propertyValue == null || !propertyValue.matches(constraintsStr)) {
                    SyntaxNodeException exception = SyntaxNodeExceptionUtils
                        .createError(String.format("Incorrect value '%s' for property '%s'",
                            propertyValue,
                            TablePropertyDefinitionUtils.getPropertyDisplayName(propertyName)), tsn);
                    tsn.addError(exception);
                    messages.add(new OpenLErrorMessage(exception));
                }
            }
        }
        return ValidationUtils.withMessages(messages);
    }

}
