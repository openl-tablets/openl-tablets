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

    MatchTraceObject(String type, ColumnMatch columnMatch, int rowIndex, int resultIndex) {
        super(type);
        this.columnMatch = columnMatch;
        this.rowIndex = rowIndex;
        this.resultIndex = resultIndex;
    }

    public MatchTraceObject(ColumnMatch columnMatch, int rowIndex, int resultIndex) {
        this("cmMatch", columnMatch, rowIndex, resultIndex);
    }

    public List<IGridRegion> getGridRegions() {
        TableRow row = columnMatch.getRows().get(rowIndex);
        List<IGridRegion> regions = new ArrayList<IGridRegion>();
        regions.add(row.get(MatchAlgorithmCompiler.VALUES)[resultIndex].getGridRegion());
        return regions;
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
