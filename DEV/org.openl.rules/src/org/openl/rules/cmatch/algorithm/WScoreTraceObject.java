package org.openl.rules.cmatch.algorithm;

import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.table.IGridRegion;

import java.util.List;

public class WScoreTraceObject extends ATableTracerNode {
    private int score;

    public WScoreTraceObject(ColumnMatch columnMatch, Object[] params) {
        super(columnMatch, params);
    }

    public String getDisplayName(int mode) {
        return ("Score: " + score);
    }

    public List<IGridRegion> getGridRegions() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getType() {
        return "wcmScore";
    }

    public void setScore(int score) {
        this.score = score;
    }
}
