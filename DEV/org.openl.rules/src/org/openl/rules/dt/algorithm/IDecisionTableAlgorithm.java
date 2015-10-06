package org.openl.rules.dt.algorithm;

import org.openl.domain.IIntIterator;
import org.openl.rules.dtx.trace.IDecisionTableTraceObject;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.TraceStack;

public interface IDecisionTableAlgorithm {

	void removeParamValuesForIndexedConditions();

	IIntIterator checkedRules(Object target, Object[] params, IRuntimeEnv env);

	IDecisionTableAlgorithm asTraceDecorator(TraceStack conditionsStack,
			IDecisionTableTraceObject traceObject);
	
	
	

}
