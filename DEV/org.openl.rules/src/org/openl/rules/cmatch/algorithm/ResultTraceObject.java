package org.openl.rules.cmatch.algorithm;

import org.openl.rules.cmatch.ColumnMatch;

public class ResultTraceObject extends MatchTraceObject {

    public ResultTraceObject(ColumnMatch columnMatch, int resultIndex) {
        super("cmResult", columnMatch, 0, resultIndex);
    }
}
