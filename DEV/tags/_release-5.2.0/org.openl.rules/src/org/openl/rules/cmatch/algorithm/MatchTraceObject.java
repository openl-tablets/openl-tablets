package org.openl.rules.cmatch.algorithm;

import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.cmatch.SubValue;
import org.openl.rules.cmatch.TableRow;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ATableTracerLeaf;
import org.openl.rules.table.IGridRegion;

public class MatchTraceObject extends ATableTracerLeaf {

    private ColumnMatch columnMatch;
    private int rowIndex;
    private int resultIndex;

    public MatchTraceObject(ColumnMatch columnMatch, int rowIndex, int resultIndex) {
        this.columnMatch = columnMatch;
        this.rowIndex = rowIndex;
        this.resultIndex = resultIndex;
    }

    @Override
    public String getUri() {
        TableRow row = columnMatch.getRows().get(rowIndex);
        SubValue sv = row.get(MatchAlgorithmCompiler.VALUES)[resultIndex];
        return sv.getStringValue().getMetaInfo().getSourceUrl();
    }

    public String getType() {
        return "cmMatch";
    }

    public String getDisplayName(int mode) {
        TableRow row = getRow();
        String operation = row.get(MatchAlgorithmCompiler.OPERATION)[0].getString();
        String checkValue = row.get(MatchAlgorithmCompiler.VALUES)[resultIndex].getString();
        return "Match: " + operation + " " + checkValue;
    }

    public IGridRegion getGridRegion() {
        TableRow row = columnMatch.getRows().get(rowIndex);
        return row.get(MatchAlgorithmCompiler.VALUES)[resultIndex].getGridRegion();
    }

    public TableSyntaxNode getTableSyntaxNode() {
        return columnMatch.getTableSyntaxNode();
    }

    protected TableRow getRow() {
        return columnMatch.getRows().get(rowIndex);
    }

    protected int getResultIndex() {
        return resultIndex;
    }
}
