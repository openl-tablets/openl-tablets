package org.openl.rules.validation;

import java.util.LinkedHashSet;
import java.util.regex.Pattern;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.constraints.RegexpValueConstraint;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.rules.table.properties.inherit.PropertiesChecker;
import org.openl.types.IOpenClass;
import org.openl.validation.ValidationResult;

/**
 * Validator for string properties that have to correspond to some regexp pattern.
 *
 * <p>A table whose value does not match is reported at the error severity of the property: a warning for
 * {@code WARN}, and an error for {@code ERROR} or when the property declares no severity.
 *
 * @author PUdalau
 */
public class RegexpPropertyValidator extends TablesValidator {
    private final String propertyName;
    private final Pattern pattern;

    public RegexpPropertyValidator(String propertyName, String constraintsStr) {
        this.propertyName = propertyName;
        this.pattern = Pattern.compile(RegexpValueConstraint.getRegexPattern(constraintsStr));
    }

    @Override
    public ValidationResult validateTables(TableSyntaxNode[] tableSyntaxNodes, IOpenClass openClass) {
        var messages = new LinkedHashSet<OpenLMessage>();
        var severity = ValidationUtils.severityOf(propertyName, Severity.ERROR);
        for (TableSyntaxNode tsn : tableSyntaxNodes) {
            if (PropertiesChecker.isPropertySuitableForTableType(propertyName,
                    tsn.getType()) && tsn.getTableProperties() != null && tsn.getTableProperties()
                    .getPropertyLevelDefinedOn(propertyName) == InheritanceLevel.TABLE) {
                var propertyValue = (String) tsn.getTableProperties().getPropertyValue(propertyName);
                if (propertyValue == null || !pattern.matcher(propertyValue).matches()) {
                    messages.add(ValidationUtils.messageOf("Incorrect value '%s' for property '%s'".formatted(
                            propertyValue,
                            TablePropertyDefinitionUtils.getPropertyDisplayName(propertyName)), severity, tsn));
                }
            }
        }
        return ValidationUtils.withMessages(messages);
    }

}
