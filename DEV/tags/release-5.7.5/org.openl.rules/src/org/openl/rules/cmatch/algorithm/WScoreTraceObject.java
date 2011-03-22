package org.openl.rules.cmatch.algorithm;

import java.util.List;

import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.table.IGridRegion;

public class WScoreTraceObject extends ATableTracerNode {
    private ColumnMatch columnMatch;
    private int score;

    public WScoreTraceObject(ColumnMatch columnMatch) {
        super(columnMatch, null);

        this.columnMatch = columnMatch;
    }

    public String getDisplayName(int mode) {
        return ("Score: " + score);
    }

    public List<IGridRegion> getGridRegions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TableSyntaxNode getTableSyntaxNode() {
        return columnMatch.getSyntaxNode();
    }

    public String getType() {
        return "wcmScore";
    }

    @Override
    public String getUri() {
        return columnMatch.getSourceUrl();
    }

    public void setScore(int score) {
        this.score = score;
    }
}
