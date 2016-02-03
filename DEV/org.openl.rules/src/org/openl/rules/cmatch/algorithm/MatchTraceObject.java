package org.openl.rules.cmatch.algorithm;

import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.cmatch.TableRow;
import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.table.IGridRegion;

public class MatchTraceObject extends ATableTracerNode {

    private IGridRegion gridRegion;
    private String checkValue;
    private String operation;

    public MatchTraceObject(ColumnMatch columnMatch, String checkValue, String operation, IGridRegion gridRegion) {
        super("cmMatch", null, columnMatch, null);
        this.gridRegion = gridRegion;
        this.checkValue = checkValue;
        this.operation = operation;
    }

    public String getCheckValue() {
        return checkValue;
    }

    public String getOperation() {
        return operation;
    }

    public IGridRegion getGridRegion() {
        return gridRegion;
    }
}
