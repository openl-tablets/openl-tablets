package org.openl.rules.dt.element;

import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.openl.OpenL;
import org.openl.binding.BindingDependencies;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.IBoundMethodNode;
import org.openl.binding.ILocalVar;
import org.openl.binding.impl.TypeBoundNode;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.binding.RulesBindingDependencies;
import org.openl.rules.dt.algorithm.DecisionTableOptimizedAlgorithm;
import org.openl.rules.dt.algorithm.DependentParametersOptimizedAlgorithm;
import org.openl.rules.dt.algorithm.evaluator.DefaultConditionEvaluator;
import org.openl.rules.dt.algorithm.evaluator.IConditionEvaluator;
import org.openl.rules.dt.algorithm.ExpressionTypeUtils;
import org.openl.rules.table.ILogicalTable;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IMethodCaller;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IParameterDeclaration;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.OpenFieldDelegator;
import org.openl.types.impl.ParameterMethodCaller;
import org.openl.types.impl.SourceCodeMethodCaller;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public class Condition extends FunctionalRow implements ICondition {

    private IMethodCaller evaluator;
    private IConditionEvaluator conditionEvaluator;

    public Condition(String name, int row, ILogicalTable decisionTable) {
        super(name, row, decisionTable);
    }

    public IConditionEvaluator getConditionEvaluator() {
        return conditionEvaluator;
    }

    public boolean isAction() {
        return false;
    }

    public boolean isCondition() {
        return true;
    }

    public IMethodCaller getEvaluator() {
        return evaluator == null ? getMethod() : evaluator;
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
    public IConditionEvaluator prepareCondition(IMethodSignature signature,
            OpenL openl,
            ComponentOpenClass componentOpenClass,
            IBindingContextDelegator bindingContextDelegator,
            RuleRow ruleRow) throws Exception {

        super.prepare(NullOpenClass.the, signature, openl, componentOpenClass, bindingContextDelegator, ruleRow);
        IBoundMethodNode methodNode = ((CompositeMethod) getMethod()).getMethodBodyBoundNode();
        IOpenSourceCodeModule source = methodNode.getSyntaxNode().getModule();

        if (StringUtils.isEmpty(source.getCode())) {
            throw SyntaxNodeExceptionUtils.createError("Cannot execute empty expression", source);
        }

        // tested in TypeInExpressionTest
        //
        if (methodNode.getChildren().length == 1 && methodNode.getChildren()[0].getChildren()[0] instanceof TypeBoundNode) {
            String message = String.format("Cannot execute expression with only type definition %s", source.getCode());
            throw SyntaxNodeExceptionUtils.createError(message, source);
        }

        IOpenClass methodType = ((CompositeMethod) getMethod()).getBodyType();

        if (isDependentOnAnyParams()) {
            if (methodType != JavaOpenClass.BOOLEAN && methodType != JavaOpenClass.getOpenClass(Boolean.class)) {
                throw SyntaxNodeExceptionUtils.createError("Condition must have boolean type if it depends on it's parameters",
                    source);
            }

            conditionEvaluator = DependentParametersOptimizedAlgorithm.makeEvaluator(this,
                signature,
                bindingContextDelegator);

            if (conditionEvaluator != null) {
                evaluator = makeOptimizedConditionMethodEvaluator(signature,
                    conditionEvaluator.getOptimizedSourceCode());
                if (evaluator == null) {
                    evaluator = makeDependentParamsIndexedConditionMethodEvaluator(signature,
                        conditionEvaluator.getOptimizedSourceCode());
                }
                return conditionEvaluator;
            }

            return conditionEvaluator = new DefaultConditionEvaluator();
        }

        IConditionEvaluator dtcev = DecisionTableOptimizedAlgorithm.makeEvaluator(this,
            methodType,
            bindingContextDelegator);

        evaluator = makeOptimizedConditionMethodEvaluator(signature);

        return conditionEvaluator = dtcev;
    }

    public DecisionValue calculateCondition(int rule, Object target, Object[] dtParams, IRuntimeEnv env) {

        Object value = getParamValues()[rule];

        if (value == null) {
            return DecisionValue.NxA_VALUE;
        }

        if (value instanceof DecisionValue) {
            return (DecisionValue) value;
        }

        Object[] params = mergeParams(target, dtParams, env, (Object[]) value);
        Object result = getMethod().invoke(target, params, env);
        Boolean res = (Boolean) result;

        // Check that condition expression has returned the not null value.
        //
        if (res == null) {
            throw new OpenLRuntimeException("Condition expression must be boolean type",
                ((CompositeMethod) getMethod()).getMethodBodyBoundNode());
        }

        if (res.booleanValue()) {
            return DecisionValue.TRUE_VALUE;
        } else {
            return DecisionValue.FALSE_VALUE;
        }
    }

    private IOpenField getLocalField(IOpenField f) {

        if (f instanceof ILocalVar) {
            return f;
        }

        if (f instanceof OpenFieldDelegator) {
            OpenFieldDelegator d = (OpenFieldDelegator) f;

            return d.getField();
        }

        return f;
    }

    private boolean isDependentOnAnyParams() {

        IParameterDeclaration[] params = getParams();

        BindingDependencies dependencies = new RulesBindingDependencies();
        ((CompositeMethod) getMethod()).updateDependency(dependencies);

        Iterator<IOpenField> iter = dependencies.getFieldsMap().values().iterator();

        while (iter.hasNext()) {

            IOpenField field = iter.next();
            field = getLocalField(field);

            if (field instanceof ILocalVar) {

                for (int i = 0; i < params.length; i++) {
                    if (params[i].getName().equals(field.getName())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private IMethodCaller makeOptimizedConditionMethodEvaluator(IMethodSignature signature) {
        String code = ((CompositeMethod) getMethod()).getMethodBodyBoundNode().getSyntaxNode().getModule().getCode();
        return makeOptimizedConditionMethodEvaluator(signature, code);
    }

    private IMethodCaller makeOptimizedConditionMethodEvaluator(IMethodSignature signature, String code) {
        for (int i = 0; i < signature.getNumberOfParameters(); i++) {
            String pname = signature.getParameterName(i);
            if (pname.equals(code)) {
                return new ParameterMethodCaller(getMethod(), i);
            }
        }
        return null;
    }

    private IMethodCaller makeDependentParamsIndexedConditionMethodEvaluator(IMethodSignature signature,
            String optimizedCode) {
        String v = ((CompositeMethod) getMethod()).getMethodBodyBoundNode().getSyntaxNode().getModule().getCode();
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

    @Override
    public void removeDebugInformation() {
        getMethod().removeDebugInformation();
    }
}
