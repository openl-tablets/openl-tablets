package org.openl.rules.dt.element;

import org.openl.binding.BindingDependencies;
import org.openl.binding.ILocalVar;
import org.openl.rules.binding.RulesBindingDependencies;
import org.openl.rules.dt.DTScale;
import org.openl.rules.dt.algorithm.evaluator.IConditionEvaluator;
import org.openl.rules.table.ILogicalTable;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenField;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.OpenFieldDelegator;
import org.openl.vm.IRuntimeEnv;

public class Condition extends FunctionalRow implements ICondition {

    private IMethodCaller evaluator;
    private IConditionEvaluator conditionEvaluator;

    public Condition(String name, int row, ILogicalTable decisionTable, DTScale.RowScale scale) {
        super(name, row, decisionTable, scale);
    }

    public IConditionEvaluator getConditionEvaluator() {
        return conditionEvaluator;
    }

    public void setConditionEvaluator(IConditionEvaluator conditionEvaluator) {
        this.conditionEvaluator = conditionEvaluator;
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

    public void setEvaluator(IMethodCaller evaluator) {
        this.evaluator = evaluator;
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

        if (Boolean.TRUE.equals(result)) {
            // True
            return DecisionValue.TRUE_VALUE;
        } else {
            // Null or False
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
        getMethod().updateDependency(dependencies);

        for (IOpenField field : dependencies.getFieldsMap().values()) {

            field = getLocalField(field);

            if (field instanceof ILocalVar) {

                for (IParameterDeclaration param : params) {
                    if (param.getName().equals(field.getName())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public void removeDebugInformation() {
        getMethod().removeDebugInformation();
    }
}
