package org.openl.rules.validation;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.openl.OpenL;
import org.openl.message.OpenLWarnMessage;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.types.OpenMethodDispatcherHelper;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.validation.ValidationResult;
import org.openl.validation.ValidationStatus;
import org.openl.validation.ValidationUtils;

public class UniquePropertyValueValidator extends TablesValidator {

    private String propertyName;

    public UniquePropertyValueValidator(String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    public ValidationResult validateTables(OpenL openl, TableSyntaxNode[] tableSyntaxNodes, IOpenClass openClass) {
        
        ExecutableRulesMethod[] executableActiveMethods = 
            selectActiveMethods(OpenMethodDispatcherHelper.extractMethods(openClass.getMethods()));

        Map<Object, ExecutableRulesMethod> values = new HashMap<Object, ExecutableRulesMethod>();
        ValidationResult validationResult = null;

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

                // String message =
                // String.format("Found tables with duplicate property '%s': %s [%s], %s [%s]",
                // propertyName,
                // existsTable.getHeaderLineValue().getValue(),
                // existsTable.getUri(),
                // tableSyntaxNode.getHeaderLineValue().getValue(),
                // tableSyntaxNode.getUri());

                String message = String.format("Found method with duplicate property '%s'", propertyName);

                if (validationResult == null) {
                    validationResult = new ValidationResult(ValidationStatus.FAIL, null);

                    OpenLWarnMessage warnMessage1 = new OpenLWarnMessage(message, existsMethod.getSyntaxNode());
                    OpenLWarnMessage warnMessage2 = new OpenLWarnMessage(message, method.getSyntaxNode());

                    ValidationUtils.addValidationMessage(validationResult, warnMessage1);
                    ValidationUtils.addValidationMessage(validationResult, warnMessage2);

                } else {
                    OpenLWarnMessage warnMessage = new OpenLWarnMessage(message, method.getSyntaxNode());
                    ValidationUtils.addValidationMessage(validationResult, warnMessage);
                }
            } else {
                values.put(value, method);
            }
        }

        // Return validation result if it not null (it is not null if at
        // least one error has occurred).
        //
        if (validationResult != null) {
            return validationResult;
        }

        return ValidationUtils.validationSuccess();
    }
    
    @SuppressWarnings("unchecked")
    private ExecutableRulesMethod[] selectActiveMethods(List<IOpenMethod> methods) {        

        Collection<IOpenMethod> outputCollection = CollectionUtils.select(methods, new Predicate() {
            
            public boolean evaluate(Object arg0) {
                IOpenMethod method = (IOpenMethod) arg0;
                
                if (method instanceof ExecutableRulesMethod) {
                    ExecutableRulesMethod executableMethod = (ExecutableRulesMethod) method;
                    if (executableMethod.getMethodProperties() == null || 
                            executableMethod.getMethodProperties().getActive() == null) {
                        // if property is not mentioned, consider it is true by default.
                        //
                        return true;
                    } else {
                        // if mentioned, return it`s value
                        //
                        return executableMethod.getMethodProperties().getActive();
                    }
                }
                // if method is not executable(e.g. instanceof OpenConstructor or instanceof GetOpenClass), 
                // we dont`t care about active property, and need to filter this one.
                //
                return false;
            }
        });

        return outputCollection.toArray(new ExecutableRulesMethod[outputCollection.size()]);
    }
}
