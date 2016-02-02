package org.openl.rules.dt.algorithm;

import org.openl.domain.IIntIterator;
import org.openl.rules.dtx.trace.DecisionTableTraceObject;
import org.openl.vm.IRuntimeEnv;

public interface IDecisionTableAlgorithm {

    void removeParamValuesForIndexedConditions();

    IIntIterator checkedRules(Object target, Object[] params, IRuntimeEnv env);

    IDecisionTableAlgorithm asTraceDecorator(DecisionTableTraceObject traceObject);

}
