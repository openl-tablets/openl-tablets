package org.openl.rules.dt.element;

import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.algorithm.evaluator.IConditionEvaluator;
import org.openl.types.IMethodCaller;
import org.openl.vm.IRuntimeEnv;

public interface ICondition extends IBaseCondition, IDecisionRow {

    DecisionValue calculateCondition(int col, Object target, Object[] dtParams, IRuntimeEnv env);

    IConditionEvaluator getConditionEvaluator();

    void setConditionEvaluator(IConditionEvaluator iConditionEvaluator);

    IMethodCaller getEvaluator();

    void setEvaluator(IMethodCaller iMethodCaller);

    boolean isDependentOnAnyParams();
    
}
