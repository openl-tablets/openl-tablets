package org.openl.rules.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.openl.message.OpenLErrorMessage;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.OpenLWarnMessage;
import org.openl.message.Severity;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.util.CollectionUtils;
import org.openl.validation.ValidationResult;

@RequiredArgsConstructor
public class UniquePropertyValueValidator extends TablesValidator {

    private final String propertyName;

    @Override
    public ValidationResult validateTables(TableSyntaxNode[] tableSyntaxNodes, IOpenClass openClass) {

        var executableActiveMethods = selectActiveMethods(
                OpenMethodDispatcherHelper.extractMethods(openClass));

        var values = new HashMap<Object, Set<ExecutableRulesMethod>>();
        var messages = new ArrayList<OpenLMessage>();

        for (ExecutableRulesMethod method : executableActiveMethods) {
            if (!method.isAlias()) {

                var methodProperties = method.getMethodProperties();

                if (methodProperties == null) {

                    // Skip current method validation.
                    //
                    continue;
                }

                // Get property value.
                //
                var value = methodProperties.getPropertyValue(propertyName);

                if (value == null) {
                    continue;
                }

                // Check that method with same property value does not exist. If
                // method with the same property value exists then create/add
                // validation error message else add current property value to list
                // of processed values.
                //
                if (values.containsKey(value)) {
                    values.get(value).add(method);
                } else {
                    var setOfExecutableRulesMethods = new HashSet<ExecutableRulesMethod>();
                    setOfExecutableRulesMethods.add(method);
                    values.put(value, setOfExecutableRulesMethods);
                }
            }
        }

        for (Map.Entry<Object, Set<ExecutableRulesMethod>> entry : values.entrySet()) {
            if (entry.getValue().size() > 1) {
                TablePropertyDefinition property = TablePropertyDefinitionUtils.getPropertyByName(propertyName);
                Severity errorSeverity = null;
                if (property != null) {
                    errorSeverity = property.getErrorSeverity();
                }
                var value = entry.getKey();
                for (ExecutableRulesMethod method : entry.getValue()) {
                    var message = getMessage(
                            "Found non-unique value '%s' for table property '%s'.".formatted(value, propertyName),
                            errorSeverity,
                            method.getSyntaxNode());
                    messages.add(message);
                }
            }
        }
        return ValidationUtils.withMessages(messages);
    }

    private OpenLMessage getMessage(String message, Severity severity, TableSyntaxNode syntaxNode) {
        if (Severity.WARN.equals(severity)) {
            if (syntaxNode != null) {
                return new OpenLWarnMessage(message, syntaxNode);
            } else {
                return OpenLMessagesUtils.newWarnMessage(message);
            }
        } else if (Severity.ERROR.equals(severity)) {
            SyntaxNodeException sne = SyntaxNodeExceptionUtils.createError(message, syntaxNode);
            // error should be put inside tsn
            //
            return new OpenLErrorMessage(sne);
        }
        // return warning in default case, e.g. severity == null
        //
        return new OpenLWarnMessage(message, syntaxNode);
    }

    private Collection<ExecutableRulesMethod> selectActiveMethods(List<IOpenMethod> methods) {
        return CollectionUtils.findAll(methods.stream()
                .filter(ExecutableRulesMethod.class::isInstance)
                .map(ExecutableRulesMethod.class::cast)
                .collect(Collectors.toList()), method -> {
            if (method.getMethodProperties() == null || method.getMethodProperties().getActive() == null) {
                // if property is not mentioned, consider it is true
                // by default.
                //
                return true;
            } else {
                // if mentioned, return it`s value
                //
                return method.getMethodProperties().getActive();
            }
        });
    }
}
