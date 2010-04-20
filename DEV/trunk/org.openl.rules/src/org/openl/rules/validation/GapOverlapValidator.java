package org.openl.rules.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.OpenL;
import org.openl.domain.IDomain;
import org.openl.message.OpenLErrorMessage;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.type.domains.DomainAdaptorFactory;
import org.openl.rules.dt.type.domains.IDomainAdaptor;
import org.openl.rules.dt.validator.DecisionTableAnalyzer;
import org.openl.rules.dt.validator.DecisionTableValidator;
import org.openl.rules.dt.validator.DesionTableValidationResult;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IOpenClass;
import org.openl.types.IParameterDeclaration;
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
                    Map<String, IDomainAdaptor> domains = gatherDomains((DecisionTable) tsn.getMember());
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

    private Map<String, IDomainAdaptor> gatherDomains(DecisionTable dt) {
        Map<String, IDomainAdaptor> domainsMap = new HashMap<String, IDomainAdaptor>();
        DecisionTableAnalyzer analyzer = new DecisionTableAnalyzer(dt);
        for (ICondition condition : dt.getConditionRows()) {
            List<IParameterDeclaration> parameters = getAllParameters(condition, analyzer);
            IDomain<?> domain = findDomainForConditionVariables(parameters, condition, analyzer);
            if (domain != null) {
                IDomainAdaptor adaptor = DomainAdaptorFactory.getAdaptor(domain);
                for (IParameterDeclaration parameter : parameters) {
                    domainsMap.put(parameter.getName(), adaptor);
                }
            }
        }
        return domainsMap;
    }

    /**
     * @return all parameter declarations: from signature and local from
     *         condition
     */
    private List<IParameterDeclaration> getAllParameters(ICondition condition, DecisionTableAnalyzer analyzer) {
        List<IParameterDeclaration> result = new ArrayList<IParameterDeclaration>();
        IParameterDeclaration[] paramDeclarations = condition.getParams();
        result.addAll(Arrays.asList(paramDeclarations));
        IParameterDeclaration[] referencedSignatureParams = analyzer.referencedSignatureParams(condition);
        result.addAll(Arrays.asList(referencedSignatureParams));
        return result;
    }

    /**
     * @param condition Condition to check for variables without domain.
     * @return <code>null</code> if there is no variables without domain
     *         otherwise the domain for variables without domain.
     */
    private IDomain<?> findDomainForConditionVariables(List<IParameterDeclaration> parameters, ICondition condition,
            DecisionTableAnalyzer analyzer) {
        IDomain<?> domain = null;
        for (IParameterDeclaration parameter : parameters) {
            domain = parameter.getType().getDomain();
            if (domain == null) {
                domain = analyzer.gatherDomainFromValues(parameter, condition);
            }
        }
        return domain;
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
