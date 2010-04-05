package org.openl.rules.dt.element;

import org.openl.OpenL;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.dt.algorithm.evaluator.IConditionEvaluator;
import org.openl.types.IMethodCaller;
import org.openl.types.IMethodSignature;
import org.openl.vm.IRuntimeEnv;

public interface ICondition extends IDecisionRow {

    DecisionValue calculateCondition(int col, Object target, Object[] dtParams, IRuntimeEnv env);

    IConditionEvaluator getConditionEvaluator();

    IMethodCaller getEvaluator();

    IConditionEvaluator prepareCondition(IMethodSignature signature,
            OpenL openl,
            ModuleOpenClass dtModule,
            IBindingContextDelegator cxtd,
            RuleRow ruleRow) throws Exception;

}
