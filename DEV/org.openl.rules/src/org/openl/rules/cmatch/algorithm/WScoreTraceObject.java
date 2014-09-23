package org.openl.rules.cmatch.algorithm;

import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.table.ATableTracerNode;

public class WScoreTraceObject extends ATableTracerNode {

    public WScoreTraceObject(ColumnMatch columnMatch, Object[] params) {
        super(columnMatch, params);
    }

    public String getDisplayName(int mode) {
        return ("Score: " + getResult());
    }

    public String getType() {
        return "wcmScore";
    }
}
