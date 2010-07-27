package org.openl.rules.cmatch.algorithm;

import java.util.List;

import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.table.IGridRegion;
import org.openl.types.IOpenMethod;

public class ColumnMatchTraceObject extends ATableTracerNode {

    private ColumnMatch columnMatch;

    public ColumnMatchTraceObject(ColumnMatch columnMatch, Object[] params) {
        super(columnMatch, params);
        this.columnMatch = columnMatch;
    }

    public String getDisplayName(int mode) {
        return "CM " + asString((IOpenMethod) getTraceObject(), mode);
    }

    public List<IGridRegion> getGridRegions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TableSyntaxNode getTableSyntaxNode() {
        return columnMatch.getTableSyntaxNode();
    }

    public String getType() {
        return "cmatch";
    }

    @Override
    public String getUri() {
        return columnMatch.getSourceUrl();
    }
}
