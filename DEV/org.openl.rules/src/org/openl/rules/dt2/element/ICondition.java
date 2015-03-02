package org.openl.rules.dt2.element;

import org.openl.OpenL;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.rules.dt2.algorithm.evaluator.IConditionEvaluator;
import org.openl.rules.dtx.IBaseCondition;
import org.openl.types.IMethodCaller;
import org.openl.types.IMethodSignature;
import org.openl.vm.IRuntimeEnv;

public interface ICondition extends IBaseCondition, IDecisionRow {

    DecisionValue calculateCondition(int col, Object target, Object[] dtParams, IRuntimeEnv env);

    IConditionEvaluator getConditionEvaluator();

    IMethodCaller getEvaluator();
    
    IConditionEvaluator prepareCondition(IMethodSignature signature,
            OpenL openl,
            ComponentOpenClass componentOpenClass,
            IBindingContextDelegator cxtd,
            RuleRow ruleRow) throws Exception;


}
