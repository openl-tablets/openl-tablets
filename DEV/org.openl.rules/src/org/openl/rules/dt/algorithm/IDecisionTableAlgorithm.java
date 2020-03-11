package org.openl.rules.dt.algorithm;

import org.openl.domain.IIntIterator;
import org.openl.vm.IRuntimeEnv;

public interface IDecisionTableAlgorithm {

    void cleanParamValuesForIndexedConditions();

    IIntIterator checkedRules(Object target, Object[] params, IRuntimeEnv env);

}
