package org.openl.rules.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.ExecutableRulesMethod;
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
                addPropertyValue(values, method);
            }
        }

        for (Map.Entry<Object, Set<ExecutableRulesMethod>> entry : values.entrySet()) {
            if (entry.getValue().size() > 1) {
                var errorSeverity = ValidationUtils.severityOf(propertyName, Severity.WARN);
                var value = entry.getKey();
                for (ExecutableRulesMethod method : entry.getValue()) {
                    var message = ValidationUtils.messageOf(
                            "Found non-unique value '%s' for table property '%s'.".formatted(value, propertyName),
                            errorSeverity,
                            method.getSyntaxNode());
                    messages.add(message);
                }
            }
        }
        return ValidationUtils.withMessages(messages);
    }

    private void addPropertyValue(Map<Object, Set<ExecutableRulesMethod>> values, ExecutableRulesMethod method) {
        var methodProperties = method.getMethodProperties();

        if (methodProperties == null) {

            // Skip current method validation.
            //
            return;
        }

        // Get property value.
        //
        var value = methodProperties.getPropertyValue(propertyName);

        if (value != null) {
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

    private Collection<ExecutableRulesMethod> selectActiveMethods(List<IOpenMethod> methods) {
        return CollectionUtils.findAll(methods.stream()
                .filter(ExecutableRulesMethod.class::isInstance)
                .map(ExecutableRulesMethod.class::cast)
                .toList(), method -> {
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
