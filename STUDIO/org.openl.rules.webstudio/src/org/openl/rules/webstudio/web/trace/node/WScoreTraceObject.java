package org.openl.rules.webstudio.web.trace.node;

import org.openl.rules.cmatch.ColumnMatch;

public class WScoreTraceObject extends ATableTracerNode {

    public WScoreTraceObject(ColumnMatch columnMatch, Object[] params) {
        super("wcmScore", null, columnMatch, params);
    }
}
