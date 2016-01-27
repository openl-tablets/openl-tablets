package org.openl.rules.cmatch.algorithm;

import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.table.ATableTracerNode;

public class WScoreTraceObject extends ATableTracerNode {

    public WScoreTraceObject(ColumnMatch columnMatch, Object[] params) {
        super("wcmScore", null, columnMatch, params);
    }
}
