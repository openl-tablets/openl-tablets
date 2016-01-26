package org.openl.rules.dtx.trace;

import org.openl.rules.table.ITableTracerObject;

public interface IDecisionTableTraceObject extends ITableTracerObject {

    Object[] getParameters();
}
