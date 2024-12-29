package org.openl.rules.dt;

import org.openl.types.Invokable;

public interface IBaseCondition extends IBaseDecisionRow {

    IBaseCondition[] EMPTY = new IBaseCondition[0];

    IBaseConditionEvaluator getConditionEvaluator();

    Invokable getEvaluator();
}
