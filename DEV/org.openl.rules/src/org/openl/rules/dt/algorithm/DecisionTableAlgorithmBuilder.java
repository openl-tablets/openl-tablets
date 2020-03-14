package org.openl.rules.dt.algorithm;

import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundMethodNode;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.TypeBoundNode;
import org.openl.binding.impl.component.ComponentBindingContext;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.DecisionTableUtils;
import org.openl.rules.dt.algorithm.evaluator.DefaultConditionEvaluator;
import org.openl.rules.dt.algorithm.evaluator.IConditionEvaluator;
import org.openl.rules.dt.data.DecisionTableDataType;
import org.openl.rules.dt.element.IAction;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.element.RuleRow;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeExceptionCollector;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IMethodCaller;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.IParameterDeclaration;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.ParameterMethodCaller;
import org.openl.types.impl.SourceCodeMethodCaller;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.StringUtils;

public class DecisionTableAlgorithmBuilder implements IAlgorithmBuilder {

    /**
     * See also "Using datatype arrays in rules by user defined index" in Reference Guide. Array can be accessed using
     * syntax: drivers[“David”], drivers[“7”].
     *
     * @see org.openl.types.impl.ArrayFieldIndex
     */
    private static final Pattern ARRAY_ACCESS_PATTERN = Pattern.compile(".+\\[.+]$");

    private IndexInfo baseInfo;
    private DecisionTable table;
    private IConditionEvaluator[] evaluators;

    private IOpenMethodHeader header;
    private OpenL openl;
    private IMethodSignature signature;
    private RuleRow ruleRow;

    public DecisionTableAlgorithmBuilder(DecisionTable decisionTable, IOpenMethodHeader header, OpenL openl) {

        this.table = decisionTable;
        this.header = header;
        this.signature = header.getSignature();
        this.openl = openl;
        this.ruleRow = table.getRuleRow();
    }

    private static String cutExpressionRoot(String expression) {
        StringTokenizer stringTokenizer = new StringTokenizer(expression, ".");
        if (stringTokenizer.hasMoreTokens()) {
            String v = stringTokenizer.nextToken();
            boolean arrayAccess = StringUtils.matches(ARRAY_ACCESS_PATTERN, v);
            if (arrayAccess) {
                v = v.substring(0, v.indexOf("["));
            }
            return v;
        }
        return expression;
    }

    static IOpenClass findExpressionType(IOpenClass type, String expression) {
        StringTokenizer stringTokenizer = new StringTokenizer(expression, ".");
        boolean isFirst = true;
        while (stringTokenizer.hasMoreTokens()) {
            String v = stringTokenizer.nextToken();
            boolean arrayAccess = StringUtils.matches(ARRAY_ACCESS_PATTERN, v);
            if (isFirst) {
                if (arrayAccess) {
                    type = type.getComponentClass();
                }
                isFirst = false;
                continue;
            }
            IOpenField field;
            if (arrayAccess) {
                v = v.substring(0, v.indexOf("["));
            }
            field = type.getField(v);
            type = field.getType();
            if (type.isArray() && arrayAccess) {
                type = type.getComponentClass();
            }
        }
        return type;
    }

    private IDecisionTableAlgorithm buildAlgorithm() {
        if (table.getDtInfo().getNumberHConditions() > 0) {

            IndexInfo vInfo = baseInfo.makeVerticalInfo();
            IndexInfo hInfo = baseInfo.makeHorizontalalInfo();

            IDecisionTableAlgorithm va = new DecisionTableOptimizedAlgorithm(evaluators, table, vInfo);
            IDecisionTableAlgorithm ha = new DecisionTableOptimizedAlgorithm(evaluators, table, hInfo);
            return new TwoDimensionalAlgorithm(va, ha);
        }

        return new DecisionTableOptimizedAlgorithm(evaluators, table, baseInfo);

    }

    @Override
    public IDecisionTableAlgorithm prepareAndBuildAlgorithm(IBindingContext bindingContext) throws Exception {
        evaluators = prepareConditions(bindingContext);
        prepareActions(bindingContext);

        baseInfo = new IndexInfo().withTable(table);
        return buildAlgorithm();
    }

    private void prepareActions(IBindingContext bindingContext) throws Exception {
        DecisionTableDataType ruleExecutionType = new DecisionTableDataType(table,
            table.getName() + "Type",
            openl,
            false);
        IBindingContext actionBindingContext = new ComponentBindingContext(bindingContext, ruleExecutionType);

        int nActions = table.getNumberOfActions();
        for (int i = 0; i < nActions; i++) {
            IAction action = table.getAction(i);
            prepareAction(action, actionBindingContext, ruleExecutionType);
        }
    }

    private void prepareAction(IAction action,
            IBindingContext actionBindingContext,
            DecisionTableDataType ruleExecutionType) throws Exception {
        action.prepareAction(header,
            signature,
            openl,
            actionBindingContext,
            ruleRow,
            ruleExecutionType,
            table.getSyntaxNode());
    }

    private IConditionEvaluator[] prepareConditions(IBindingContext bindingContext) throws Exception {
        DecisionTableDataType ruleExecutionType = new DecisionTableDataType(table,
            table.getName() + "Type",
            openl,
            true);
        IBindingContext conditionBindingContext = new ComponentBindingContext(bindingContext, ruleExecutionType);

        int nConditions = table.getNumberOfConditions();
        final IConditionEvaluator[] evaluators = new IConditionEvaluator[nConditions];

        SyntaxNodeExceptionCollector syntaxNodeExceptionCollector = new SyntaxNodeExceptionCollector();
        for (int i = 0; i < nConditions; i++) {
            final int index = i;
            syntaxNodeExceptionCollector
                .run(() -> evaluators[index] = prepareCondition(ruleExecutionType, conditionBindingContext, index));
        }
        syntaxNodeExceptionCollector.throwIfAny("Error:");

        return evaluators;
    }

    /**
     * Since version 4.0.2 the Condition can have an optimized form. In this case the Condition Expression(CE) can have
     * type that is different from boolean. For details consult DTOptimizedAlgorithm class
     *
     * The algorithm for Condition Expression parsing will work like this:
     * <p>
     * 1) Compile CE as <code>void</code> expression with all the Condition Parameters(CP). Report errors and return, if
     * any
     * <p>
     * 2) Check if the expression depends on any of the condition parameters, if it does, it is not intended to be
     * optimized (at least not in this version). In this case it has to have <code>boolean</code> type.
     * <p>
     * 3) Try to find possible expression/optimization for the combination of CE and CP types. See DTOptimizedAlgorithm
     * for the full set of available optimizations. If not found - raise exception.
     * <p>
     * 4) Attach, expression/optimization to the Condition; change the expression header to remove CP, because they are
     * not needed.
     *
     * @see DecisionTableOptimizedAlgorithm
     * @since 4.0.2
     */

    private IConditionEvaluator prepareCondition(DecisionTableDataType ruleExecutionType,
            IBindingContext bindingContext,
            int index) throws Exception {
        ICondition condition = table.getCondition(index);

        condition.prepare(NullOpenClass.the,
            signature,
            openl,
            bindingContext,
            ruleRow,
            ruleExecutionType,
            table.getSyntaxNode());
        condition.setConditionParametersUsed(checkConditionParameterUsedInExpression(condition));
        condition.setRuleIdOrRuleNameUsed(checkRuleIdOrRuleNameInExpression(condition));

        IBoundMethodNode methodNode = ((CompositeMethod) condition.getMethod()).getMethodBodyBoundNode();
        IOpenSourceCodeModule source = methodNode.getSyntaxNode().getModule();

        if (StringUtils.isEmpty(source.getCode())) {
            throw SyntaxNodeExceptionUtils.createError("Cannot execute empty expression.", source);
        }

        // tested in TypeInExpressionTest
        //
        IBoundNode[] children = methodNode.getChildren();
        if (children != null && children.length == 1 && children[0].getChildren()[0] instanceof TypeBoundNode) {
            String message = String.format("Cannot execute expression with only type definition '%s'.",
                source.getCode());
            throw SyntaxNodeExceptionUtils.createError(message, source);
        }

        IOpenClass methodType = ((CompositeMethod) condition.getMethod()).getBodyType();

        if (condition.isDependentOnAnyParams() || condition.isRuleIdOrRuleNameUsed()) {
            if (!JavaOpenClass.BOOLEAN.equals(methodType) && !JavaOpenClass.getOpenClass(Boolean.class)
                .equals(methodType)) {
                throw SyntaxNodeExceptionUtils.createError(
                    "Condition expression must return a boolean type if it uses condition parameters.",
                    source);
            }

            IConditionEvaluator conditionEvaluator = DependentParametersOptimizedAlgorithm
                .makeEvaluator(condition, signature, bindingContext);

            if (conditionEvaluator != null) {
                condition.setConditionEvaluator(conditionEvaluator);
                IMethodCaller evaluator = makeOptimizedConditionMethodEvaluator(condition,
                    signature,
                    conditionEvaluator.getOptimizedSourceCode());
                condition.setEvaluator(evaluator);
                if (evaluator == null) {
                    condition.setEvaluator(makeDependentParamsIndexedConditionMethodEvaluator(condition,
                        signature,
                        conditionEvaluator.getOptimizedSourceCode()));
                }
            } else {
                conditionEvaluator = new DefaultConditionEvaluator();
                condition.setConditionEvaluator(conditionEvaluator);
            }
            return conditionEvaluator;
        } else {
            IConditionEvaluator dtcev = DecisionTableOptimizedAlgorithm
                .makeEvaluator(condition, methodType, bindingContext);
            condition.setEvaluator(makeOptimizedConditionMethodEvaluator(condition, signature));
            condition.setConditionEvaluator(dtcev);
            return dtcev;
        }
    }

    private static boolean checkConditionParameterUsedInExpression(ICondition condition) {
        List<IdentifierNode> identifierNodes = DecisionTableUtils.retrieveIdentifierNodes(condition);
        for (IParameterDeclaration condParam : condition.getParams()) {
            if (identifierNodes.stream()
                .anyMatch(identifierNode -> condParam.getName().equals(identifierNode.getIdentifier()))) {
                return true;
            }
        }
        return false;
    }

    private static boolean checkRuleIdOrRuleNameInExpression(ICondition condition) {
        List<IdentifierNode> identifierNodes = DecisionTableUtils.retrieveIdentifierNodes(condition);
        return identifierNodes.stream()
            .anyMatch(e -> "$Rule".equals(e.getIdentifier()) || "$RuleId".equals(e.getIdentifier()));
    }

    private IMethodCaller makeOptimizedConditionMethodEvaluator(ICondition condition, IMethodSignature signature) {
        return makeOptimizedConditionMethodEvaluator(condition,
            signature,
            DecisionTableUtils.getConditionSourceCode(condition));
    }

    private IMethodCaller makeOptimizedConditionMethodEvaluator(ICondition condition,
            IMethodSignature signature,
            String code) {
        for (int i = 0; i < signature.getNumberOfParameters(); i++) {
            String pname = signature.getParameterName(i);
            if (pname.equals(code)) {
                return new ParameterMethodCaller(condition.getMethod(), i);
            }
        }
        return null;
    }

    private IMethodCaller makeDependentParamsIndexedConditionMethodEvaluator(ICondition condition,
            IMethodSignature signature,
            String optimizedCode) {
        String v = ((CompositeMethod) condition.getMethod()).getMethodBodyBoundNode()
            .getSyntaxNode()
            .getModule()
            .getCode();
        if (optimizedCode != null && !optimizedCode.equals(v)) {
            String p = cutExpressionRoot(optimizedCode);
            for (int i = 0; i < signature.getNumberOfParameters(); i++) {
                String pname = signature.getParameterName(i);
                if (pname.equals(p)) {
                    IOpenClass type = findExpressionType(signature.getParameterType(i), optimizedCode);
                    return new SourceCodeMethodCaller(signature, type, optimizedCode);
                }
            }
        }
        return null;
    }
}
