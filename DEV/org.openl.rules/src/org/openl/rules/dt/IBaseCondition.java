package org.openl.rules.dt;

import org.openl.rules.table.ILogicalTable;
import org.openl.types.IMethodCaller;

public interface IBaseCondition extends IBaseDecisionRow {

    IBaseConditionEvaluator getConditionEvaluator();

    IMethodCaller getEvaluator();

    ILogicalTable getValueCell(int ruleIndex);

    void removeDebugInformation();

}
