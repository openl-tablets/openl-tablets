package org.openl.rules.webstudio.web.trace.node;

import org.openl.rules.cmatch.ColumnMatch;
import org.openl.runtime.IRuntimeContext;

public class WScoreTraceObject extends ATableTracerNode {

    WScoreTraceObject(ColumnMatch columnMatch, Object[] params, IRuntimeContext context) {
        super("wcmScore", null, columnMatch, params, context);
    }
}
