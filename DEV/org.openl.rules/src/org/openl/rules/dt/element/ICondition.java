package org.openl.rules.dt.element;

import org.openl.rules.dt.algorithm.evaluator.IConditionEvaluator;
import org.openl.rules.dt.storage.StorageInfo;
import org.openl.rules.dtx.IBaseCondition;
import org.openl.types.IMethodCaller;
import org.openl.vm.IRuntimeEnv;

public interface ICondition extends IBaseCondition, IDecisionRow {

    DecisionValue calculateCondition(int col, Object target, Object[] dtParams, IRuntimeEnv env);

    IConditionEvaluator getConditionEvaluator();

    IMethodCaller getEvaluator();
    
//    IConditionEvaluator prepareCondition(IMethodSignature signature,
//            OpenL openl,
//            ComponentOpenClass componentOpenClass,
//            IBindingContextDelegator cxtd,
//            RuleRow ruleRow) throws Exception;

	boolean isDependentOnAnyParams();

	void setConditionEvaluator(IConditionEvaluator iConditionEvaluator);

	void setEvaluator(IMethodCaller iMethodCaller);

	
	StorageInfo getStorageInfo(int paramN);

}
