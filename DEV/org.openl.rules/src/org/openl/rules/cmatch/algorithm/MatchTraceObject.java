package org.openl.rules.cmatch.algorithm;

import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.cmatch.TableRow;
import org.openl.rules.table.ATableTracerLeaf;

public class MatchTraceObject extends ATableTracerLeaf {

    private ColumnMatch columnMatch;
    private int rowIndex;
    private int resultIndex;

    MatchTraceObject(String type, ColumnMatch columnMatch, int rowIndex, int resultIndex) {
        super(type);
        this.columnMatch = columnMatch;
        this.rowIndex = rowIndex;
        this.resultIndex = resultIndex;
    }

    public MatchTraceObject(ColumnMatch columnMatch, int rowIndex, int resultIndex) {
        this("cmMatch", columnMatch, rowIndex, resultIndex);
    }

    public int getResultIndex() {
        return resultIndex;
    }

    public TableRow getRow() {
        return columnMatch.getRows().get(rowIndex);
    }

    @Override
    public String getUri() {
        return columnMatch.getSyntaxNode().getUri();
    }
}
