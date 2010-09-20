package org.openl.rules.dt.element;

import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.openl.OpenL;
import org.openl.binding.BindingDependencies;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.ILocalVar;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.dt.algorithm.DecisionTableOptimizedAlgorithm;
import org.openl.rules.dt.algorithm.evaluator.DefaultConditionEvaluator;
import org.openl.rules.dt.algorithm.evaluator.IConditionEvaluator;
import org.openl.rules.table.IGridTable;
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
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public class Condition extends FunctionalRow implements ICondition {

    private IMethodCaller evaluator;
    private IConditionEvaluator conditionEvaluator;

    public Condition(String name, int row, IGridTable decisionTable) {
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
     * Parameters(CP). Report errors anad return, if any
     * <p>
     * 2) Check if the expression depends on any of the parameters, if it does,
     * it is not intended to be optimized (at least not in this version). In
     * this case it has to have <code>boolean</code> type.
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
            ModuleOpenClass module,
            IBindingContextDelegator bindingContextDelegator,
            RuleRow ruleRow) throws Exception {

        super.prepare(NullOpenClass.the, signature, openl, module, bindingContextDelegator, ruleRow);
        
        IOpenSourceCodeModule source = ((CompositeMethod) getMethod()).getMethodBodyBoundNode().getSyntaxNode().getModule();

        if (StringUtils.isEmpty(source.getCode())){
            throw SyntaxNodeExceptionUtils.createError("Cannot execute empty expression", source);
        }
        
        IOpenClass methodType = ((CompositeMethod) getMethod()).getBodyType();

        if (isDependentOnAnyParams()) {
            if (methodType != JavaOpenClass.BOOLEAN) {
                throw new Exception("Condition must have boolean type if it depends on it's parameters");
            }

            return conditionEvaluator = new DefaultConditionEvaluator();
        }

        evaluator = makeOptimizedConditionMethodEvaluator(signature);
        IConditionEvaluator dtcev = DecisionTableOptimizedAlgorithm.makeEvaluator(this, methodType);

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
        Boolean res = (Boolean) getMethod().invoke(target, params, env);

        if (res == null || res.booleanValue()) {
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

        BindingDependencies dependencies = new BindingDependencies();
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

        for (int i = 0; i < signature.getNumberOfParameters(); i++) {
            String pname = signature.getParameterName(i);
            if (pname.equals(code)) {
                return new ParameterMethodCaller(getMethod(), i);
            }
        }

        return null;
    }

}
