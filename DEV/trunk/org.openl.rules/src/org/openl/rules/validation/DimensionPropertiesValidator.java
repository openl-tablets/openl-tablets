package org.openl.rules.validation;

import org.openl.OpenL;
import org.openl.exception.OpenLRuntimeException;
import org.openl.message.OpenLErrorMessage;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.type.domains.DimensionPropertiesDomainsCollector;
import org.openl.rules.dt.validator.DesionTableValidationResult;
import org.openl.rules.dt.validator.DecisionTableValidator;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IOpenClass;

import org.openl.validation.ValidationResult;
import org.openl.validation.ValidationStatus;
import org.openl.validation.ValidationUtils;


public class DimensionPropertiesValidator extends TablesValidator {
    
    private static final String VALIDATION_FAILED = "Validation failed for dispatcher table";
    
    @Override
    public ValidationResult validateTables(OpenL openl, TableSyntaxNode[] tableSyntaxNodes, IOpenClass openClass) {        
        ValidationResult validationResult = null;        
        for (TableSyntaxNode tsn : tableSyntaxNodes) {
            if (tsn.getDisplayName() != null && tsn.getDisplayName().contains(DispatcherTableBuilder.DEFAULT_METHOD_NAME)) {
                
                DimensionPropertiesDomainsCollector propDomainCollector = new DimensionPropertiesDomainsCollector();
                propDomainCollector.gatherPropertiesDomains(tableSyntaxNodes);
                
                DesionTableValidationResult dtValidResult = null;
                try {
                    dtValidResult = DecisionTableValidator.validateTable((DecisionTable)tsn.getMember(), 
                            propDomainCollector.getGatheredPropertiesDomains(), openClass);     
                } catch (Exception t) {
                    throw new OpenLRuntimeException(VALIDATION_FAILED, t);
                }
                if (dtValidResult != null && dtValidResult.hasProblems()) {
                    tsn.setValidationResult(dtValidResult);
                    if (validationResult == null) {
                        validationResult = new ValidationResult(ValidationStatus.FAIL); 
                    } 
                    SyntaxNodeException error = new SyntaxNodeException(dtValidResult.toString(), null, tsn);
                    tsn.addError(error);
                    ValidationUtils.addValidationMessage(validationResult, new OpenLErrorMessage(error));
                } 
            }                        
        }
        if (validationResult != null) {
            return validationResult;
        }
        return ValidationUtils.validationSuccess();
    }    

}
