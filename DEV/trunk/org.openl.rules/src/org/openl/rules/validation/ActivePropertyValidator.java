package org.openl.rules.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.OpenL;
import org.openl.message.OpenLErrorMessage;
import org.openl.message.OpenLWarnMessage;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.DimensionPropertiesMethodKey;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.types.OpenMethodDispatcherHelper;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.rules.method.ExecutableRulesMethod;
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
        
        // Group methods not TableSyntaxNodes as we may have dependent modules, and no sources for them, 
        // represented in current module. The only information about dependency methods contains in openClass.
        //
        Map<DimensionPropertiesMethodKey, List<ExecutableRulesMethod>> groupedMethods = 
            groupExecutableMethods(OpenMethodDispatcherHelper.extractMethods(openClass.getMethods()));
        
        for (DimensionPropertiesMethodKey key : groupedMethods.keySet()) {
            List<ExecutableRulesMethod> methodsGroup = groupedMethods.get(key);
            boolean activeTableWasFound = false;
            
            for (ExecutableRulesMethod executableMethod : methodsGroup) {
                if (executableMethod instanceof TestSuiteMethod) {
                    // all tests are active by default
                    //
                    activeTableWasFound = true;
                    break;
                }
                if (executableMethod.getMethodProperties() != null && 
                        Boolean.TRUE.equals(executableMethod.getMethodProperties().getActive())) {
                    if (activeTableWasFound) {
                        if (validationResult == null) {
                            validationResult = new ValidationResult(ValidationStatus.FAIL);
                        }
                        SyntaxNodeException exception = new SyntaxNodeException(ODD_ACTIVE_TABLE_MESSAGE, null, 
                            executableMethod.getSyntaxNode());
                        executableMethod.getSyntaxNode().addError(exception);
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
                        methodsGroup.get(0).getSyntaxNode()));
            }
        }
        

        if (validationResult != null) {
            return validationResult;
        } else {
            return ValidationUtils.validationSuccess();
        }
    }
    
    private Map<DimensionPropertiesMethodKey, List<ExecutableRulesMethod>> groupExecutableMethods(List<IOpenMethod> methods) {
        Map<DimensionPropertiesMethodKey, List<ExecutableRulesMethod>> groupedMethods = 
            new HashMap<DimensionPropertiesMethodKey, List<ExecutableRulesMethod>>();
        
        for (IOpenMethod method : methods) {
            if (method instanceof ExecutableRulesMethod) {
                ExecutableRulesMethod executableMethod = (ExecutableRulesMethod) method;
                DimensionPropertiesMethodKey key = new DimensionPropertiesMethodKey(executableMethod);
                if (!groupedMethods.containsKey(key)) {
                    groupedMethods.put(key, new ArrayList<ExecutableRulesMethod>());
                }
                groupedMethods.get(key).add(executableMethod);
            }
        }
        return groupedMethods;
    }
}
