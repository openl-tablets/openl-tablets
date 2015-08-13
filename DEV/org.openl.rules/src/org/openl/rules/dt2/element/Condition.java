package org.openl.rules.dt2.element;

import java.util.Iterator;

import org.openl.binding.BindingDependencies;
import org.openl.binding.ILocalVar;
import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.binding.RulesBindingDependencies;
import org.openl.rules.dt2.DTScale;
import org.openl.rules.dt2.algorithm.evaluator.IConditionEvaluator;
import org.openl.rules.table.ILogicalTable;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenField;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.OpenFieldDelegator;
import org.openl.vm.IRuntimeEnv;

public class Condition extends FunctionalRow implements ICondition {

    private IMethodCaller evaluator;
    public void setEvaluator(IMethodCaller evaluator) {
		this.evaluator = evaluator;
	}

	public void setConditionEvaluator(IConditionEvaluator conditionEvaluator) {
		this.conditionEvaluator = conditionEvaluator;
	}

	private IConditionEvaluator conditionEvaluator;

    public Condition(String name, int row, ILogicalTable decisionTable, DTScale.RowScale scale) {
        super(name, row, decisionTable, scale);
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

 
    public DecisionValue calculateCondition(int ruleN, Object target, Object[] dtParams, IRuntimeEnv env) {

        if (isEmpty(ruleN)) {
            return DecisionValue.NxA_VALUE;
        }

        // if (value instanceof DecisionValue) {
        // return (DecisionValue) value;
        // }

        Object[] params = mergeParams(target, dtParams, env, ruleN);
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

    public boolean isDependentOnAnyParams() {

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



}
