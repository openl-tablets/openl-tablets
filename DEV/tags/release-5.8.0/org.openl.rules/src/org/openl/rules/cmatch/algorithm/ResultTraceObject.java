package org.openl.rules.cmatch.algorithm;

import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.cmatch.TableRow;

public class ResultTraceObject extends MatchTraceObject {

    public ResultTraceObject(ColumnMatch columnMatch, int resultIndex) {
        super(columnMatch, 0, resultIndex);
    }

    @Override
    public String getDisplayName(int mode) {
        TableRow row = getRow();

        String resultValue = row.get(MatchAlgorithmCompiler.VALUES)[getResultIndex()].getString();
        return "Result: " + resultValue;
    }

    @Override
    public String getType() {
        return "cmResult";
    }

}
