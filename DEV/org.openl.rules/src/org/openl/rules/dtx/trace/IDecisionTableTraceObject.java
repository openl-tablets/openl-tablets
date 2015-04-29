package org.openl.rules.dtx.trace;

import org.openl.rules.dtx.IDecisionTable;
import org.openl.rules.table.ITableTracerObject;

public interface IDecisionTableTraceObject extends ITableTracerObject {


    public IDecisionTable getDecisionTable();

	public Object[] getParameters();
}
