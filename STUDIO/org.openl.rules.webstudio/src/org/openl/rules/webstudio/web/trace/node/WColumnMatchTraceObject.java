package org.openl.rules.webstudio.web.trace.node;

import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.table.ATableTracerNode;

public class WColumnMatchTraceObject extends ATableTracerNode {

    public WColumnMatchTraceObject(ColumnMatch columnMatch, Object[] params) {
        super("wcmatch", "WCM", columnMatch, params);
    }
}
