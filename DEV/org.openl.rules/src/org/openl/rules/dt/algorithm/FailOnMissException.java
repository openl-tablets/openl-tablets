package org.openl.rules.dt.algorithm;

import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.dt.DecisionTable;

public class FailOnMissException extends OpenLRuntimeException {

    private static final long serialVersionUID = -4344185808917149412L;

    public FailOnMissException(String message, DecisionTable decisionTable) {
        super(message, decisionTable == null ? null : decisionTable.getSyntaxNode());
    }

}
