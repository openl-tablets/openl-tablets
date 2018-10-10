package org.openl.rules.webstudio.web.trace.node;

import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.cmatch.TableRow;
import org.openl.rules.cmatch.algorithm.MatchAlgorithmCompiler;
import org.openl.rules.table.IGridRegion;

public class ResultTraceObject extends ATableTracerNode {
    private IGridRegion gridRegion;

    ResultTraceObject(ColumnMatch columnMatch, IGridRegion gridRegion) {
        super("cmResult", null, columnMatch, null);
        this.gridRegion = gridRegion;
    }

    public IGridRegion getGridRegion() {
        return gridRegion;
    }

    static ResultTraceObject create(Object... args) {
        ColumnMatch columnMatch = (ColumnMatch) args[0];
        int resultIndex = (Integer) args[1];
        Object result = args[2];
        TableRow tableRow = columnMatch.getRows().get(0);
        IGridRegion gridRegion = tableRow.get(MatchAlgorithmCompiler.VALUES)[resultIndex].getGridRegion();
        ResultTraceObject traceObject = new ResultTraceObject(columnMatch, gridRegion);
        traceObject.setResult(result);
        return traceObject;
    }
}
