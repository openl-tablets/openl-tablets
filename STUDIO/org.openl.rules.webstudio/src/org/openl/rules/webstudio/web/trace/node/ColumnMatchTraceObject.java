package org.openl.rules.webstudio.web.trace.node;

import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.table.ATableTracerNode;

public class ColumnMatchTraceObject extends ATableTracerNode {

    public ColumnMatchTraceObject(ColumnMatch columnMatch, Object[] params) {
        super("cmatch", "CM", columnMatch, params);
    }
}
