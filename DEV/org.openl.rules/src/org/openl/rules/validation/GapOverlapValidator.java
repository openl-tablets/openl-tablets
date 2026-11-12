package org.openl.rules.validation;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.openl.binding.IBoundNode;
import org.openl.binding.impl.ErrorBoundNode;
import org.openl.domain.IDomain;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.IDecisionTable;
import org.openl.rules.dt.algorithm.evaluator.DomainCanNotBeDefined;
import org.openl.rules.dt.type.domains.DomainAdaptorFactory;
import org.openl.rules.dt.type.domains.IDomainAdaptor;
import org.openl.rules.dt.validator.DecisionTableAnalyzer;
import org.openl.rules.dt.validator.DecisionTableValidationResult;
import org.openl.rules.dt.validator.DecisionTableValidator;
import org.openl.rules.enumeration.ValidateDTEnum;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.CompositeMethod;
import org.openl.validation.ValidationResult;

/**
 * Gap/overlap analysis for Decision Tables with property "validateDT" == "on".
 *
 * @author PUdalau
 */
public class GapOverlapValidator extends TablesValidator {

    private static final String VALIDATION_FAILED = "Validation failed for node : ";
    private static final String UNSUPPORTED_CONDITION = "The check of uncovered and overlapped cases (validateDT) is skipped: "
            + "it supports only conditions that compare an input with whole numbers or with a list of values.";

    @Override
    public ValidationResult validateTables(TableSyntaxNode[] tableSyntaxNodes, IOpenClass openClass) {
        var allModuleMethods = OpenMethodDispatcherHelper.extractMethods(openClass);

        var messages = new LinkedHashSet<OpenLMessage>();

        for (IOpenMethod method : allModuleMethods) {
            if (method instanceof ExecutableRulesMethod executableMethod && isValidatableMethod(executableMethod)) {
                // can cast to DecisionTable, as validateDT property belongs
                // only to DT.
                //
                var decisionTable = (IDecisionTable) executableMethod;
                var dtValidResult = validate(messages, openClass, decisionTable);
                if (dtValidResult != null && dtValidResult.hasProblems()) {
                    decisionTable.getSyntaxNode().setValidationResult(dtValidResult);
                    if (dtValidResult.hasErrors()) {
                        addError(messages, decisionTable.getSyntaxNode(), dtValidResult.toString());
                    } else {
                        messages.add(OpenLMessagesUtils.newWarnMessage(dtValidResult.toString(),
                                decisionTable.getSyntaxNode()));
                    }
                }
            }
        }
        return ValidationUtils.withMessages(messages);
    }

    private void addError(Collection<OpenLMessage> messages, TableSyntaxNode tableSyntaxNode, String message) {
        SyntaxNodeException sne = SyntaxNodeExceptionUtils.createError(message, tableSyntaxNode);
        messages.add(OpenLMessagesUtils.newErrorMessage(sne));
    }

    private DecisionTableValidationResult validate(Collection<OpenLMessage> messages,
                                                   IOpenClass openClass,
                                                   IDecisionTable decisionTable) {
        DecisionTableValidationResult dtValidResult = null;
        try {
            var domains = gatherDomains(decisionTable);
            dtValidResult = DecisionTableValidator.validateTable(decisionTable, domains, openClass);
        } catch (DomainCanNotBeDefined e) {
            // Not a failure of the table: the check cannot tell which values such a condition covers.
            messages.add(OpenLMessagesUtils.newWarnMessage(UNSUPPORTED_CONDITION, decisionTable.getSyntaxNode()));
        } catch (Exception t) {
            var errorMessage = "%s%s.Reason : %s".formatted(
                    VALIDATION_FAILED,
                    decisionTable.getSyntaxNode().getDisplayName(),
                    t.getMessage());
            addError(messages, decisionTable.getSyntaxNode(), errorMessage);
        }
        return dtValidResult;
    }

    private Map<String, IDomainAdaptor> gatherDomains(IDecisionTable dt) throws DomainCanNotBeDefined {
        var domainsMap = new HashMap<String, IDomainAdaptor>();
        var analyzer = new DecisionTableAnalyzer(dt);

        for (IBaseCondition condition : dt.getConditionRows()) {
            var pd = analyzer.referencedSignatureParams(condition);
            for (IParameterDeclaration iParameterDeclaration : pd) {
                IDomain<?> domain = iParameterDeclaration.getType().getDomain();
                if (domain == null) {
                    domain = condition.getConditionEvaluator().getRuleParameterDomain(condition);
                    IDomainAdaptor adaptor = DomainAdaptorFactory.getAdaptor(domain);
                    domainsMap.put(iParameterDeclaration.getName(), adaptor);
                }
            }

            var cparams = condition.getParams();

            for (var i = 0; i < cparams.length; i++) {
                IDomain<?> domain = cparams[i].getType().getDomain();
                if (domain == null) {
                    domain = condition.getConditionEvaluator().getConditionParameterDomain(i, condition);
                    if (domain != null) {
                        IDomainAdaptor adaptor = DomainAdaptorFactory.getAdaptor(domain);
                        domainsMap.put(
                                DecisionTableValidator.getUniqueConditionParamName(condition, cparams[i].getName()),
                                adaptor);
                    }

                }

            }

        }
        return domainsMap;
    }

    private static boolean hasCompileErrors(IDecisionTable decisionTable) {
        return Arrays.stream(decisionTable.getConditionRows())
                .map(IBaseCondition::getMethod)
                .anyMatch(method -> !(method instanceof CompositeMethod expression) || hasErrorNode(
                        expression.getMethodBodyBoundNode()));
    }

    private static boolean hasErrorNode(@Nullable IBoundNode node) {
        return node == null || node instanceof ErrorBoundNode
                || Arrays.stream(node.getChildren()).anyMatch(GapOverlapValidator::hasErrorNode);
    }

    /**
     * Tells whether the table asks for the check and its conditions compiled. A table with a compile error has
     * reported it, and the check cannot run a condition that did not compile.
     */
    private static boolean isValidatableMethod(ExecutableRulesMethod executableMethod) {
        return executableMethod.getMethodProperties() != null && ValidateDTEnum.ON.equals(
                executableMethod.getMethodProperties().getValidateDT()) && !hasCompileErrors(
                        (IDecisionTable) executableMethod);
    }
}
