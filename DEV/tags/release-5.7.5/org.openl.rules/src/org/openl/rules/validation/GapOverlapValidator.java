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
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.types.OpenMethodDispatcherHelper;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
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
        List<IOpenMethod> allModuleMethods = OpenMethodDispatcherHelper.extractMethods(openClass.getMethods());
        
        for (IOpenMethod method : allModuleMethods) {
            if (method instanceof ExecutableRulesMethod) {
                ExecutableRulesMethod executableMethod = (ExecutableRulesMethod) method;
                if (isValidatableMethod(executableMethod)) {
                    // can cast to DecisionTable, as validateDT property belongs only to DT.
                    //
                    DecisionTable decisionTable = (DecisionTable) executableMethod;
                    DesionTableValidationResult dtValidResult = validate(openClass, decisionTable);
                    if (dtValidResult != null && dtValidResult.hasProblems()) {
                        decisionTable.getSyntaxNode().setValidationResult(dtValidResult);
                        addError(decisionTable.getSyntaxNode(), dtValidResult.toString());
                    }
                }
            }            
        }    
        if (validationResult != null) {
            return validationResult;
        }
        return ValidationUtils.validationSuccess();
    }

    private DesionTableValidationResult validate(IOpenClass openClass, DecisionTable decisionTable) {
        DesionTableValidationResult dtValidResult = null;
        try {
            Map<String, IDomainAdaptor> domains = gatherDomains(decisionTable);
            dtValidResult = DecisionTableValidator.validateTable(decisionTable, domains, openClass);
        } catch (Exception t) {
            String errorMessage = String.format("%s%s.Reason : %s", VALIDATION_FAILED, 
                decisionTable.getSyntaxNode().getDisplayName(), t.getMessage());
            addError(decisionTable.getSyntaxNode(), errorMessage);
        }
        return dtValidResult;
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
    
    private static boolean isValidatableMethod(ExecutableRulesMethod executableMethod) {
        return executableMethod.getMethodProperties() != null && "on".equals(executableMethod.getMethodProperties().getPropertyValueAsString("validateDT"));        
    }
}
