package org.openl.rules.cmatch.algorithm;

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

    @Override
    public String getUri() {
        return columnMatch.getSourceUrl();
    }

    public String getType() {
        return "cmatch";
    }

    public String getDisplayName(int mode) {
        return "CM " + asString((IOpenMethod)getTraceObject(), mode);
    }

    public IGridRegion getGridRegion() {
        // TODO Auto-generated method stub
        return null;
    }

    public TableSyntaxNode getTableSyntaxNode() {
        return columnMatch.getTableSyntaxNode();
    }
}
