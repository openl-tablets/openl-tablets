package org.openl.rules.webstudio.web.trace.node;

import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.cmatch.MatchNode;
import org.openl.rules.cmatch.TableRow;
import org.openl.rules.cmatch.algorithm.MatchAlgorithmCompiler;
import org.openl.rules.table.IGridRegion;

public class MatchTraceObject extends ATableTracerNode {

    private IGridRegion gridRegion;
    private String checkValue;
    private String operation;

    MatchTraceObject(ColumnMatch columnMatch, String checkValue, String operation, IGridRegion gridRegion) {
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

    static MatchTraceObject create(Object... args) {
        ColumnMatch columnMatch = (ColumnMatch) args[0];
        MatchNode node = (MatchNode) args[1];
        int resultIndex = (Integer) args[2];
        Object result = args[3];
        int rowIndex = node.getRowIndex();
        TableRow row = columnMatch.getRows().get(rowIndex);
        IGridRegion gridRegion = row.get(MatchAlgorithmCompiler.VALUES)[resultIndex].getGridRegion();
        String operation = row.get(MatchAlgorithmCompiler.OPERATION)[0].getString();
        String checkValue = row.get(MatchAlgorithmCompiler.VALUES)[resultIndex].getString();
        MatchTraceObject trObj = new MatchTraceObject(columnMatch, checkValue, operation, gridRegion);
        trObj.setResult(result);
        return trObj;
    }
}
