package org.openl.rules.cmatch.algorithm;

import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.table.ATableTracerNode;
import org.openl.types.IOpenMethod;

public class ColumnMatchTraceObject extends ATableTracerNode {

    public ColumnMatchTraceObject(ColumnMatch columnMatch, Object[] params) {
        super("cmatch", columnMatch, params);
    }

    public String getDisplayName(int mode) {
        return "CM " + asString((IOpenMethod) getTraceObject(), mode);
    }
}
