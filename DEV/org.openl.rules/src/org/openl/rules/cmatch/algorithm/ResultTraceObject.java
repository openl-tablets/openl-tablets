package org.openl.rules.cmatch.algorithm;

import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.table.IGridRegion;

public class ResultTraceObject extends ATableTracerNode {
    private IGridRegion gridRegion;

    public ResultTraceObject(ColumnMatch columnMatch, IGridRegion gridRegion) {
        super("cmResult", null, columnMatch, null);
        this.gridRegion = gridRegion;
    }

    public IGridRegion getGridRegion() {
        return gridRegion;
    }
}
