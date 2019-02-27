package org.openl.rules.dt;

import org.openl.rules.table.ILogicalTable;
import org.openl.types.IMethodCaller;

public interface IBaseCondition extends IBaseDecisionRow {

    IBaseCondition[] EMPTY = new IBaseCondition[0];

    IBaseConditionEvaluator getConditionEvaluator();

    IMethodCaller getEvaluator();

    ILogicalTable getValueCell(int ruleIndex);

    void removeDebugInformation();

}
