package org.openl.rules.cmatch.algorithm;

import java.util.ArrayList;
import java.util.List;

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

    public String getDisplayName(int mode) {
        TableRow row = getRow();
        String operation = row.get(MatchAlgorithmCompiler.OPERATION)[0].getString();
        String checkValue = row.get(MatchAlgorithmCompiler.VALUES)[resultIndex].getString();
        return "Match: " + operation + " " + checkValue;
    }

    public List<IGridRegion> getGridRegions() {
        TableRow row = columnMatch.getRows().get(rowIndex);
        List<IGridRegion> regions = new ArrayList<IGridRegion>();
        regions.add(row.get(MatchAlgorithmCompiler.VALUES)[resultIndex].getGridRegion());
        return regions;
    }

    protected int getResultIndex() {
        return resultIndex;
    }

    protected TableRow getRow() {
        return columnMatch.getRows().get(rowIndex);
    }

    public TableSyntaxNode getTableSyntaxNode() {
        return columnMatch.getSyntaxNode();
    }

    public String getType() {
        return "cmMatch";
    }

    @Override
    public String getUri() {
        TableRow row = columnMatch.getRows().get(rowIndex);
        SubValue sv = row.get(MatchAlgorithmCompiler.VALUES)[resultIndex];
        return sv.getStringValue().getMetaInfo().getSourceUrl();
    }
}
