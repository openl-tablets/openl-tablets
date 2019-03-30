package org.openl.rules.validation;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.openl.OpenL;
import org.openl.message.OpenLErrorMessage;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLWarnMessage;
import org.openl.message.Severity;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.method.ITablePropertiesMethod;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.util.CollectionUtils;
import org.openl.validation.ValidationResult;

public class UniquePropertyValueValidator extends TablesValidator {

    private String propertyName;

    public UniquePropertyValueValidator(String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    public ValidationResult validateTables(OpenL openl, TableSyntaxNode[] tableSyntaxNodes, IOpenClass openClass) {

        ExecutableRulesMethod[] executableActiveMethods = selectActiveMethods(
            OpenMethodDispatcherHelper.extractMethods(openClass));

        Map<Object, ExecutableRulesMethod> values = new HashMap<>();
        Collection<OpenLMessage> messages = new LinkedHashSet<>();

        for (ExecutableRulesMethod method : executableActiveMethods) {

            ITableProperties methodProperties = method.getMethodProperties();

            if (methodProperties == null) {

                // Skip current method validation.
                //
                continue;
            }

            // Get property value.
            //
            Object value = methodProperties.getPropertyValue(propertyName);

            if (value == null) {
                continue;
            }

            // Check that method with same property value doesn't exist. If
            // method with the same property value exists then create/add
            // validation error message else add current property value to list
            // of processed values.
            //
            if (values.containsKey(value)) {

                ExecutableRulesMethod existsMethod = values.get(value);

                String message = String.format("Found method with duplicate property '%s'", propertyName);

                TablePropertyDefinition property = TablePropertyDefinitionUtils.getPropertyByName(propertyName);

                Severity errorSeverity = null;
                if (property != null) {
                    errorSeverity = property.getErrorSeverity();
                }

                OpenLMessage message1 = getMessage(message, errorSeverity, existsMethod.getSyntaxNode());
                OpenLMessage message2 = getMessage(message, errorSeverity, method.getSyntaxNode());

                messages.add(message1);
                messages.add(message2);

            } else {
                values.put(value, method);
            }
        }

        return ValidationUtils.withMessages(messages);
    }

    private OpenLMessage getMessage(String message, Severity severity, TableSyntaxNode syntaxNode) {
        if (Severity.WARN.equals(severity)) {
            return new OpenLWarnMessage(message, syntaxNode);
        } else if (Severity.ERROR.equals(severity)) {
            SyntaxNodeException sne = SyntaxNodeExceptionUtils.createError(message, syntaxNode);
            // error should be put inside tsn
            //
            syntaxNode.addError(sne);
            return new OpenLErrorMessage(sne);
        }
        // return warning in default case, e.g. severity == null
        //
        return new OpenLWarnMessage(message, syntaxNode);
    }

    private ExecutableRulesMethod[] selectActiveMethods(List<IOpenMethod> methods) {

        List<IOpenMethod> outputCollection = CollectionUtils.findAll(methods,
            new CollectionUtils.Predicate<IOpenMethod>() {

                public boolean evaluate(IOpenMethod method) {
                    if (method instanceof ITablePropertiesMethod) {
                        ITablePropertiesMethod executableMethod = (ITablePropertiesMethod) method;
                        if (executableMethod.getMethodProperties() == null || executableMethod.getMethodProperties()
                            .getActive() == null) {
                            // if property is not mentioned, consider it is true
                            // by default.
                            //
                            return true;
                        } else {
                            // if mentioned, return it`s value
                            //
                            return executableMethod.getMethodProperties().getActive();
                        }
                    }
                    // if method is not executable(e.g. instanceof
                    // OpenConstructor or instanceof GetOpenClass),
                    // we dont`t care about active property, and need to filter
                    // this one.
                    //
                    return false;
                }
            });

        return outputCollection.toArray(new ExecutableRulesMethod[outputCollection.size()]);
    }
}
