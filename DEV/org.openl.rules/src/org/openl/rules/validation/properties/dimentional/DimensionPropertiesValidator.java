package org.openl.rules.validation.properties.dimentional;

import org.openl.OpenL;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLWarnMessage;
import org.openl.rules.dt.IDecisionTable;
import org.openl.rules.dt.type.domains.DimensionPropertiesDomainsCollector;
import org.openl.rules.dt.type.domains.IDomainAdaptor;
import org.openl.rules.dt.validator.DecisionTableValidator;
import org.openl.rules.dt.validator.DecisionTableValidationResult;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.ITablePropertiesMethod;
import org.openl.rules.validation.TablesValidator;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.validation.ValidationResult;
import org.openl.rules.validation.ValidationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DimensionPropertiesValidator extends TablesValidator {

    private static final String VALIDATION_FAILED = "Validation failed for dispatcher table";

    @SuppressWarnings("unused")
    @Override
    public ValidationResult validateTables(OpenL openl, TableSyntaxNode[] tableSyntaxNodes, IOpenClass openClass) {
        ValidationResult validationResult = null;

        // FIXME: currently validation of dispatcher tables is disabled
        // because of "match by default" case(when context value == null
        // the all tables with matches by corresponding property).
        //
        // check {context value} == null || {matching expression} was
        // introduced and validation mechanism does not works with such
        // expressions.

        // Map<String, IDomainAdaptor> propertiesDomains =
        // getDomainsForDimensionalProperties(OpenMethodDispatcherHelper.extractMethods(openClass.getMethods()));
        //
        // for (TableSyntaxNode tsn : tableSyntaxNodes) {
        // // search for generated dispatcher decision table for dimension properties.
        // //
        // if (isDimensionPropertiesDispatcherTable(tsn)) {
        //
        // OpenLMessage validationMessage = null;//validateDecisionTable(tsn, propertiesDomains, openClass);
        // if (validationMessage != null) {
        // if (validationResult == null) {
        // validationResult = new ValidationResult(ValidationStatus.FAIL);
        // }
        // ValidationUtils.addValidationMessage(validationResult, validationMessage);
        // }
        // }
        // }
        // if (validationResult != null) {
        // return validationResult;
        // }
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
    @SuppressWarnings("unused")
    private OpenLMessage validateDecisionTable(TableSyntaxNode tsn,
            Map<String, IDomainAdaptor> propertiesDomains,
            IOpenClass openClass) {

        DecisionTableValidationResult tableValidationResult = validate(tsn, propertiesDomains, openClass);

        return createMessage(tsn, tableValidationResult);
    }

    /**
     * Validate decision table. Throws {@link OpenlNotCheckedException} if there were any errors during validation.
     * 
     * @param tsn generated DT with dimension properties.
     * @param propertiesDomains domains for dimension properties.
     * @param openClass Open class for whole module.
     * @return {@link DecisionTableValidationResult}
     */
    private DecisionTableValidationResult validate(TableSyntaxNode tsn,
            Map<String, IDomainAdaptor> propertiesDomains,
            IOpenClass openClass) {
        DecisionTableValidationResult tableValidationResult = null;
        try {
            tableValidationResult = DecisionTableValidator
                .validateTable((IDecisionTable) tsn.getMember(), propertiesDomains, openClass);
        } catch (Exception t) {
            throw new OpenlNotCheckedException(VALIDATION_FAILED, t);
        }
        return tableValidationResult;
    }

    private OpenLMessage createMessage(TableSyntaxNode tsn, DecisionTableValidationResult tableValidationResult) {
        OpenLMessage validationMessage = null;
        if (tableValidationResult != null && tableValidationResult.hasProblems()) {
            tsn.setValidationResult(tableValidationResult);
            // changed validation message severity to WARNING
            //
            // SyntaxNodeException error = new SyntaxNodeException(tableValidationResult.toString(), null, tsn);
            // tsn.addError(error);
            // validationMessage = new OpenLErrorMessage(error);
            validationMessage = new OpenLWarnMessage(tableValidationResult.toString(), tsn);
        }
        return validationMessage;
    }

    /**
     * Check if {@link TableSyntaxNode} represents generated dispatcher decision table for dimension properties.
     * 
     * @param tsn {@link TableSyntaxNode}
     * @return true if {@link TableSyntaxNode} represents generated dispatcher decision table for dimension properties.
     */
    private boolean isDimensionPropertiesDispatcherTable(TableSyntaxNode tsn) {
        return tsn.getDisplayName() != null && tsn.getDisplayName()
            .contains(
                DispatcherTablesBuilder.DEFAULT_DISPATCHER_TABLE_NAME) && tsn.getMember() instanceof IDecisionTable;
    }

    private Map<String, IDomainAdaptor> getDomainsForDimensionalProperties(List<IOpenMethod> methods) {

        DimensionPropertiesDomainsCollector domainCollector = new DimensionPropertiesDomainsCollector();

        Map<String, IDomainAdaptor> gatheredPropertiesDomains = domainCollector
            .gatherPropertiesDomains(getMethodProperties(methods));
        return gatheredPropertiesDomains;
    }

    /**
     * Gets properties for all methods in module.
     * 
     * @param methods all module methods.
     * 
     * @return properties for all methods in module.
     */
    private List<Map<String, Object>> getMethodProperties(List<IOpenMethod> methods) {
        List<Map<String, Object>> properties = new ArrayList<Map<String, Object>>();
        for (IOpenMethod method : methods) {
            if (method instanceof ITablePropertiesMethod) {
                properties.add(((ITablePropertiesMethod) method).getProperties());
            }
        }
        return properties;
    }

}
