package org.openl.rules.validation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.OpenL;
import org.openl.domain.IDomain;
import org.openl.message.OpenLErrorMessage;
import org.openl.message.OpenLWarnMessage;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.IDecisionTable;
import org.openl.rules.dt.type.domains.DomainAdaptorFactory;
import org.openl.rules.dt.type.domains.IDomainAdaptor;
import org.openl.rules.dt.validator.DecisionTableAnalyzer;
import org.openl.rules.dt.validator.DecisionTableValidator;
import org.openl.rules.dt.validator.DesionTableValidationResult;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.types.OpenMethodDispatcherHelper;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IParameterDeclaration;
import org.openl.validation.ValidationResult;
import org.openl.validation.ValidationStatus;

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
                    // can cast to DecisionTable, as validateDT property belongs
                    // only to DT.
                    //
                    IDecisionTable decisionTable = (IDecisionTable) executableMethod;
                    DesionTableValidationResult dtValidResult = validate(openClass, decisionTable);
                    if (dtValidResult != null && dtValidResult.hasProblems()) {
                        decisionTable.getSyntaxNode().setValidationResult(dtValidResult);
                        if (dtValidResult.hasErrors())
                            addError(decisionTable.getSyntaxNode(), dtValidResult.toString());
                        else
                            addWarning(decisionTable.getSyntaxNode(), dtValidResult.toString());
                    }
                }
            }
        }
        if (validationResult != null) {
            return validationResult;
        }
        return ValidationUtils.validationSuccess();
    }

    private DesionTableValidationResult validate(IOpenClass openClass, IDecisionTable decisionTable) {
        DesionTableValidationResult dtValidResult = null;
        try {
            Map<String, IDomainAdaptor> domains = gatherDomains(decisionTable);
            dtValidResult = DecisionTableValidator.validateTable(decisionTable, domains, openClass);
        } catch (Exception t) {
            String errorMessage = String.format("%s%s.Reason : %s", VALIDATION_FAILED, decisionTable.getSyntaxNode()
                .getDisplayName(), t.getMessage());
            addError(decisionTable.getSyntaxNode(), errorMessage);
        }
        return dtValidResult;
    }

    private Map<String, IDomainAdaptor> gatherDomains(IDecisionTable dt) throws Exception {
        Map<String, IDomainAdaptor> domainsMap = new HashMap<String, IDomainAdaptor>();
        DecisionTableAnalyzer analyzer = new DecisionTableAnalyzer(dt);

        for (IBaseCondition condition : dt.getConditionRows()) {
            // List<IParameterDeclaration> parameters =
            // getAllParameters(condition, analyzer);

            IParameterDeclaration[] pd = analyzer.referencedSignatureParams(condition);
            for (int i = 0; i < pd.length; i++) {
                IDomain<?> domain = pd[i].getType().getDomain();
                if (domain == null) {
                    domain = condition.getConditionEvaluator().getRuleParameterDomain(condition);
                    IDomainAdaptor adaptor = DomainAdaptorFactory.getAdaptor(domain);
                    domainsMap.put(pd[i].getName(), adaptor);
                }
            }

            IParameterDeclaration[] cparams = condition.getParams();

            for (int i = 0; i < cparams.length; i++) {
                IDomain<?> domain = cparams[i].getType().getDomain();
                if (domain == null) {
                    domain = condition.getConditionEvaluator().getConditionParameterDomain(i, condition);
                    if (domain != null) {
                        IDomainAdaptor adaptor = DomainAdaptorFactory.getAdaptor(domain);
                        domainsMap.put(DecisionTableValidator.getUniqueConditionParamName(condition, cparams[i].getName()), adaptor);
                    }

                }

            }

        }
        return domainsMap;
    }

    /**
     * @return all parameter declarations: from signature and local from
     *         condition
     */
    // private List<IParameterDeclaration> getAllParameters(ICondition
    // condition, DecisionTableAnalyzer analyzer) {
    // List<IParameterDeclaration> result = new
    // ArrayList<IParameterDeclaration>();
    // IParameterDeclaration[] paramDeclarations = condition.getParams();
    // result.addAll(Arrays.asList(paramDeclarations));
    // IParameterDeclaration[] referencedSignatureParams =
    // analyzer.referencedSignatureParams(condition);
    // result.addAll(Arrays.asList(referencedSignatureParams));
    // return result;
    // }

    /**
     * @param condition Condition to check for variables without domain.
     * @return <code>null</code> if there is no variables without domain
     *         otherwise the domain for variables without domain.
     */
    // private IDomain<?>
    // findDomainForConditionVariables(List<IParameterDeclaration> parameters,
    // ICondition condition,
    // DecisionTableAnalyzer analyzer) {
    // IDomain<?> domain = null;
    // for (IParameterDeclaration parameter : parameters) {
    // domain = parameter.getType().getDomain();
    // if (domain == null) {
    // domain = analyzer.gatherDomainFromValues(parameter, condition);
    // }
    // }
    // return domain;
    // }

    private void addError(TableSyntaxNode sourceNode, String message) {
        if (validationResult == null) {
            validationResult = new ValidationResult(ValidationStatus.FAIL);
        }
        SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, sourceNode);
        sourceNode.addError(error);
        ValidationUtils.addValidationMessage(validationResult, new OpenLErrorMessage(error));
    }

    private void addWarning(TableSyntaxNode sourceNode, String message) {
        if (validationResult == null) {
            validationResult = new ValidationResult(ValidationStatus.FAIL);
        }
//        SyntaxNodeException error = new SyntaxNodeException(message, null, sourceNode);
//        sourceNode.addError(error);
        ValidationUtils.addValidationMessage(validationResult, new OpenLWarnMessage(message, sourceNode));
    }
    
    
    private static boolean isValidatableMethod(ExecutableRulesMethod executableMethod) {
        return executableMethod.getMethodProperties() != null && "on".equals(executableMethod.getMethodProperties()
            .getPropertyValueAsString("validateDT"));
    }
}
