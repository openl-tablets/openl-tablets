package org.openl.rules.dt.element;

import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.algorithm.evaluator.IConditionEvaluator;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.IMethodCaller;
import org.openl.vm.IRuntimeEnv;

public interface ICondition extends IBaseCondition, IDecisionRow {

    DecisionValue calculateCondition(int col, Object target, Object[] dtParams, IRuntimeEnv env);

    @Override
    IConditionEvaluator getConditionEvaluator();

    void setConditionEvaluator(IConditionEvaluator iConditionEvaluator);

    @Override
    IMethodCaller getEvaluator();

    void setEvaluator(IMethodCaller iMethodCaller);

    boolean isDependentOnAnyParams();

    int getNumberOfEmptyRules(int paramIndex);

    IOpenSourceCodeModule getUserDefinedExpressionSource();

    /**
     * Identifier to detect whatever condition parameter is used in expression
     * @param conditionParametersUsed {@code true} when it's used, otherwise false
     */
    void setConditionParametersUsed(boolean conditionParametersUsed);

}
