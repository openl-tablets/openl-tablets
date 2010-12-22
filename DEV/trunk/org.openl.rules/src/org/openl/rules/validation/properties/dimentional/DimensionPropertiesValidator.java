package org.openl.rules.validation.properties.dimentional;

import java.util.Map;

import org.openl.OpenL;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLWarnMessage;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.type.domains.DimensionPropertiesDomainsCollector;
import org.openl.rules.dt.type.domains.IDomainAdaptor;
import org.openl.rules.dt.validator.DesionTableValidationResult;
import org.openl.rules.dt.validator.DecisionTableValidator;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.validation.TablesValidator;
import org.openl.types.IOpenClass;

import org.openl.validation.ValidationResult;
import org.openl.validation.ValidationStatus;
import org.openl.validation.ValidationUtils;

public class DimensionPropertiesValidator extends TablesValidator {
    
    private static final String VALIDATION_FAILED = "Validation failed for dispatcher table";
    
    @Override
    public ValidationResult validateTables(OpenL openl, TableSyntaxNode[] tableSyntaxNodes, IOpenClass openClass) {        
        ValidationResult validationResult = null;  
        
        Map<String, IDomainAdaptor> propertiesDomains = getDomainsForDimensionalProperties(tableSyntaxNodes);
        
        for (TableSyntaxNode tsn : tableSyntaxNodes) {
            if (isDimensionPropertiesDispatcherTable(tsn)) {
                OpenLMessage validationMessage = validateDecisionTable(tsn, propertiesDomains, openClass);
                if (validationMessage != null) {
                    if (validationResult == null) {
                        validationResult = new ValidationResult(ValidationStatus.FAIL); 
                    } 
                    ValidationUtils.addValidationMessage(validationResult, validationMessage);
                }
            }                        
        }
        if (validationResult != null) {
            return validationResult;
        }
        return ValidationUtils.validationSuccess();
    }
    
    /**
     * Validate table and create message, explaining validation result.
     * 
     * @param tsn generated DT with dimension properties.
     * @param propertiesDomains domains for dimension properties.
     * @param openClass Open class for whole module.
     * @return message, explaining validation result
     */
    private OpenLMessage validateDecisionTable(TableSyntaxNode tsn, Map<String, IDomainAdaptor> propertiesDomains, 
            IOpenClass openClass) {
        
        DesionTableValidationResult tableValidationResult = validate(tsn, propertiesDomains, openClass);
        
        return createMessage(tsn, tableValidationResult);        
    }
    
    /**
     * Validate decision table. Throws {@link OpenlNotCheckedException} if there were any errors 
     * during validation.
     * 
     * @param tsn generated DT with dimension properties.
     * @param propertiesDomains domains for dimension properties.
     * @param openClass Open class for whole module.
     * @return {@link DesionTableValidationResult}
     */
    private DesionTableValidationResult validate(TableSyntaxNode tsn,  Map<String, IDomainAdaptor> propertiesDomains,
            IOpenClass openClass) {
        DesionTableValidationResult tableValidationResult = null;
        try {
            tableValidationResult = DecisionTableValidator.validateTable((DecisionTable)tsn.getMember(), 
                    propertiesDomains, openClass);     
        } catch (Exception t) {
            throw new OpenlNotCheckedException(VALIDATION_FAILED, t);
        }
        return tableValidationResult;
    }
    
    
    private OpenLMessage createMessage(TableSyntaxNode tsn, DesionTableValidationResult tableValidationResult) {
        OpenLMessage validationMessage = null;
        if (tableValidationResult != null && tableValidationResult.hasProblems()) {
            tsn.setValidationResult(tableValidationResult);
            // changed validation message severity to WARNING
            //
//            SyntaxNodeException error = new SyntaxNodeException(tableValidationResult.toString(), null, tsn);
//            tsn.addError(error);            
//            validationMessage = new OpenLErrorMessage(error);
            validationMessage = new OpenLWarnMessage(tableValidationResult.toString(), tsn);
        }
        return validationMessage;
    }

    private boolean isDimensionPropertiesDispatcherTable(TableSyntaxNode tsn) {
        return tsn.getDisplayName() != null && tsn.getDisplayName().contains(DispatcherTableBuilder.DEFAULT_DISPATCHER_TABLE_NAME) && tsn.getMember() instanceof DecisionTable;
    }

    private Map<String, IDomainAdaptor> getDomainsForDimensionalProperties(TableSyntaxNode[] tableSyntaxNodes) {
        DimensionPropertiesDomainsCollector domainCollector = new DimensionPropertiesDomainsCollector();
        domainCollector.gatherPropertiesDomains(tableSyntaxNodes);
        Map<String, IDomainAdaptor> gatheredPropertiesDomains = domainCollector.getGatheredPropertiesDomains();
        return gatheredPropertiesDomains;
    }    

}
