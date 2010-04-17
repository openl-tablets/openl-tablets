package org.openl.rules.validation;

import java.util.Map;

import org.openl.OpenL;
import org.openl.message.OpenLErrorMessage;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.type.IDomainAdaptor;
import org.openl.rules.dt.validator.DecisionTableValidator;
import org.openl.rules.dt.validator.DesionTableValidationResult;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IOpenClass;
import org.openl.validation.ValidationResult;
import org.openl.validation.ValidationStatus;
import org.openl.validation.ValidationUtils;

/**
 * Gap/overlap analysis for Decision Tables with property "validateDT" == "on".
 * 
 * @author PUdalau
 */
public class GapOverlapValidator extends TablesValidator {

    private static final String VALIDATION_FAILED = "Validation failed for node : ";

    private ValidationResult validationResult;

    @Override
    public ValidationResult validateTables(OpenL openl, TableSyntaxNode[] tableSyntaxNodes, IOpenClass openClass) {
        validationResult = null;

        for (TableSyntaxNode tsn : tableSyntaxNodes) {
            if (isValidatableNode(tsn)) {
                DesionTableValidationResult dtValidResult = null;
                try {
                    Map<String, IDomainAdaptor> domains = gatherDomains(tsn);
                    dtValidResult = DecisionTableValidator.validateTable((DecisionTable) tsn.getMember(), domains,
                            openClass);
                } catch (Exception t) {
                    addError(tsn, VALIDATION_FAILED + tsn.getDisplayName() + ". Reason : " + t.getMessage());
                }
                if (dtValidResult != null && dtValidResult.hasProblems()) {
                    tsn.setValidationResult(dtValidResult);
                    addError(tsn, dtValidResult.toString());
                }
            }
        }
        if (validationResult != null) {
            return validationResult;
        }
        return ValidationUtils.validationSuccess();
    }
    
    private Map<String, IDomainAdaptor> gatherDomains(TableSyntaxNode tsn){
        //TODO: get not-specifed domains from table values.
        return null;
    }

    private void addError(TableSyntaxNode sourceNode, String message) {
        if (validationResult == null) {
            validationResult = new ValidationResult(ValidationStatus.FAIL);
        }
        SyntaxNodeException error = new SyntaxNodeException(message, null, sourceNode);
        sourceNode.addError(error);
        ValidationUtils.addValidationMessage(validationResult, new OpenLErrorMessage(error));
    }

    private static boolean isValidatableNode(TableSyntaxNode tsn) {
        if (tsn.getMember() instanceof DecisionTable) {
            return !tsn.hasErrors() && "on".equals(tsn.getTableProperties().getPropertyValueAsString("validateDT"));
        } else {
            return false;
        }
    }
}
