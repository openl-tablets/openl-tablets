package org.openl.rules.dt.algorithm;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.IBoundMethodNode;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.TypeBoundNode;
import org.openl.binding.impl.component.ComponentBindingContext;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.algorithm.evaluator.DefaultConditionEvaluator;
import org.openl.rules.dt.algorithm.evaluator.IConditionEvaluator;
import org.openl.rules.dt.data.DecisionTableDataType;
import org.openl.rules.dt.element.IAction;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.element.RuleRow;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.Runnable;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionCollector;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IMethodCaller;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.ParameterMethodCaller;
import org.openl.types.impl.SourceCodeMethodCaller;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.StringUtils;

public class DecisionTableAlgorithmBuilder implements IAlgorithmBuilder {

    protected IndexInfo baseInfo;
    protected DecisionTable table;
    protected IConditionEvaluator[] evaluators;

    IOpenMethodHeader header;
    OpenL openl;
    ComponentOpenClass componentOpenClass;
    ComponentOpenClass module;
    IBindingContext bindingContext;
    IMethodSignature signature;
    private RuleRow ruleRow;

    public DecisionTableAlgorithmBuilder(DecisionTable decisionTable,
            IOpenMethodHeader header,
            OpenL openl,
            ComponentOpenClass module,
            IBindingContext bindingContext) {

        this.table = decisionTable;
        this.header = header;
        this.signature = header.getSignature();
        this.openl = openl;
        this.module = module;
        this.bindingContext = bindingContext;
        this.ruleRow = table.getRuleRow();
    }

    public IDecisionTableAlgorithm buildAlgorithm() throws SyntaxNodeException {
        if (isTwoDimensional(table)) {
            IDecisionTableAlgorithm va = makeVerticalAlgorithm();
            IDecisionTableAlgorithm ha = makeHorizontalAlgorithm();
            TwoDimensionalAlgorithm twoD = new TwoDimensionalAlgorithm(va, ha);
            return twoD;
        }

        return makeFullAlgorithm();

    }

    protected IDecisionTableAlgorithm makeHorizontalAlgorithm() throws SyntaxNodeException {

        IndexInfo hInfo = baseInfo.makeHorizontalalInfo();

        return new DecisionTableOptimizedAlgorithm(evaluators, table, hInfo);
    }

    protected IDecisionTableAlgorithm makeFullAlgorithm() throws SyntaxNodeException {

        return new DecisionTableOptimizedAlgorithm(evaluators, table, baseInfo);
    }

    protected IDecisionTableAlgorithm makeVerticalAlgorithm() throws SyntaxNodeException {

        IndexInfo vInfo = baseInfo.makeVerticalInfo();

        return new DecisionTableOptimizedAlgorithm(evaluators, table, vInfo);
    }

    protected boolean isTwoDimensional(DecisionTable table2) {
        return table.getDtInfo().getNumberHConditions() > 0;
    }

    @Override
    public IDecisionTableAlgorithm prepareAndBuildAlgorithm() throws Exception {
        evaluators = prepareConditions();

        prepareActions();

        baseInfo = new IndexInfo().withTable(table);
        return buildAlgorithm();
    }

    protected DecisionTableDataType getRuleExecutionType(OpenL openl) {
        return new DecisionTableDataType(table, table.getName() + "Type", openl);
    }

    protected void prepareActions() throws Exception {

        DecisionTableDataType ruleExecutionType = getRuleExecutionType(openl);

        IBindingContext actionBindingContext = new ComponentBindingContext(bindingContext,
            ruleExecutionType);

        int nActions = table.getNumberOfActions();
        for (int i = 0; i < nActions; i++) {
            IAction action = table.getAction(i);
            prepareAction(action, actionBindingContext, ruleExecutionType);
        }
    }
    

    protected void prepareAction(IAction action,
            IBindingContext actionBindingContext,
            DecisionTableDataType ruleExecutionType) throws Exception {
       
        action.prepareAction(header,
            signature,
            openl,
            componentOpenClass,
            actionBindingContext,
            ruleRow,
            ruleExecutionType);

    }

    protected IConditionEvaluator[] prepareConditions() throws Exception {
        int nConditions = table.getNumberOfConditions();
        final IConditionEvaluator[] evaluators = new IConditionEvaluator[nConditions];

        SyntaxNodeExceptionCollector syntaxNodeExceptionCollector = new SyntaxNodeExceptionCollector();
        for (int i = 0; i < nConditions; i++) {
            final int index = i;
            syntaxNodeExceptionCollector.run(new Runnable() {
                @Override
                public void run() throws Exception {
                    ICondition condition = table.getCondition(index);
                    evaluators[index] = prepareCondition(condition);
                }
            });
        }
        syntaxNodeExceptionCollector.throwIfAny("Error:");

        return evaluators;
    }

    /**
     * Since version 4.0.2 the Condition can have an optimized form. In this
     * case the Condition Expression(CE) can have type that is different from
     * boolean. For details consult DTOptimizedAlgorithm class
     * 
     * The algorithm for Condition Expression parsing will work like this:
     * <p>
     * 1) Compile CE as <code>void</code> expression with all the Condition
     * Parameters(CP). Report errors and return, if any
     * <p>
     * 2) Check if the expression depends on any of the condition parameters, if
     * it does, it is not intended to be optimized (at least not in this
     * version). In this case it has to have <code>boolean</code> type.
     * <p>
     * 3) Try to find possible expression/optimization for the combination of CE
     * and CP types. See DTOptimizedAlgorithm for the full set of available
     * optimizations. If not found - raise exception.
     * <p>
     * 4) Attach, expression/optimization to the Condition; change the
     * expression header to remove CP, because they are not needed.
     * 
     * @see DecisionTableOptimizedAlgorithm
     * @since 4.0.2
     */

    protected IConditionEvaluator prepareCondition(ICondition condition) throws Exception {
        // return condition.prepareCondition(signature, openl,
        // componentOpenClass, bindingContextDelegator, ruleRow);

        condition.prepare(NullOpenClass.the, signature, openl, componentOpenClass, bindingContext, ruleRow);
        IBoundMethodNode methodNode = ((CompositeMethod) condition.getMethod()).getMethodBodyBoundNode();
        IOpenSourceCodeModule source = methodNode.getSyntaxNode().getModule();

        if (StringUtils.isEmpty(source.getCode())) {
            throw SyntaxNodeExceptionUtils.createError("Cannot execute empty expression", source);
        }

        // tested in TypeInExpressionTest
        //
        IBoundNode[] children = methodNode.getChildren();
        if (children != null && children.length == 1 && children[0].getChildren()[0] instanceof TypeBoundNode) {
            String message = String.format("Cannot execute expression with only type definition %s", source.getCode());
            throw SyntaxNodeExceptionUtils.createError(message, source);
        }

        IOpenClass methodType = ((CompositeMethod) condition.getMethod()).getBodyType();

        IConditionEvaluator conditionEvaluator = condition.getConditionEvaluator();
        IMethodCaller evaluator = condition.getEvaluator();
        if (condition.isDependentOnAnyParams()) {
            if (methodType != JavaOpenClass.BOOLEAN && methodType != JavaOpenClass.getOpenClass(Boolean.class)) {
                throw SyntaxNodeExceptionUtils
                    .createError("Condition must have boolean type if it depends on it's parameters", source);
            }

            condition.setConditionEvaluator(conditionEvaluator = DependentParametersOptimizedAlgorithm
                .makeEvaluator(condition, signature, bindingContext));

            if (conditionEvaluator != null) {
                condition.setEvaluator(evaluator = makeOptimizedConditionMethodEvaluator(condition,
                    signature,
                    conditionEvaluator.getOptimizedSourceCode()));
                if (evaluator == null) {
                    condition.setEvaluator(evaluator = makeDependentParamsIndexedConditionMethodEvaluator(condition,
                        signature,
                        conditionEvaluator.getOptimizedSourceCode()));
                }
                return conditionEvaluator;
            }

            condition.setConditionEvaluator(conditionEvaluator = new DefaultConditionEvaluator());
            return conditionEvaluator;
        }

        IConditionEvaluator dtcev = DecisionTableOptimizedAlgorithm.makeEvaluator(condition,
            methodType,
            bindingContext); 

        condition.setEvaluator(evaluator = makeOptimizedConditionMethodEvaluator(condition, signature));

        condition.setConditionEvaluator(conditionEvaluator = dtcev);
        return conditionEvaluator;

    }

    protected IMethodCaller makeOptimizedConditionMethodEvaluator(ICondition condition, IMethodSignature signature) {
        String code = ((CompositeMethod) condition.getMethod()).getMethodBodyBoundNode()
            .getSyntaxNode()
            .getModule()
            .getCode();
        return makeOptimizedConditionMethodEvaluator(condition, signature, code);
    }

    protected IMethodCaller makeOptimizedConditionMethodEvaluator(ICondition condition,
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

    protected IMethodCaller makeDependentParamsIndexedConditionMethodEvaluator(ICondition condition,
            IMethodSignature signature,
            String optimizedCode) {
        String v = ((CompositeMethod) condition.getMethod()).getMethodBodyBoundNode()
            .getSyntaxNode()
            .getModule()
            .getCode();
        if (optimizedCode != null && !optimizedCode.equals(v)) {
            String p = ExpressionTypeUtils.cutExpressionRoot(optimizedCode);
            for (int i = 0; i < signature.getNumberOfParameters(); i++) {
                String pname = signature.getParameterName(i);
                if (pname.equals(p)) {
                    IOpenClass type = ExpressionTypeUtils.findExpressionType(signature.getParameterType(i),
                        optimizedCode);
                    return new SourceCodeMethodCaller(signature, type, optimizedCode);
                }
            }
        }
        return null;
    }

}
