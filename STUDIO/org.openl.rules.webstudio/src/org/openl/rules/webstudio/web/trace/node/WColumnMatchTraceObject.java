package org.openl.rules.webstudio.web.trace.node;

import org.openl.rules.cmatch.ColumnMatch;

public class WColumnMatchTraceObject extends ATableTracerNode {

    public WColumnMatchTraceObject(ColumnMatch columnMatch, Object[] params) {
        super("wcmatch", "WCM", columnMatch, params);
    }
}
