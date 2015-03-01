package org.openl.rules.dtx;

import org.openl.types.IMethodCaller;


public interface IBaseCondition extends IBaseDecisionRow {

	IBaseConditionEvaluator getConditionEvaluator();

	IMethodCaller getEvaluator();



}
