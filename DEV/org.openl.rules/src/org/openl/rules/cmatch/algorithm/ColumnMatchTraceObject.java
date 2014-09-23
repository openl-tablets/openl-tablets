package org.openl.rules.cmatch.algorithm;

import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.table.IGridRegion;
import org.openl.types.IOpenMethod;

import java.util.List;

public class ColumnMatchTraceObject extends ATableTracerNode {

    public ColumnMatchTraceObject(ColumnMatch columnMatch, Object[] params) {
        super(columnMatch, params);
    }

    public String getDisplayName(int mode) {
        return "CM " + asString((IOpenMethod) getTraceObject(), mode);
    }

    public List<IGridRegion> getGridRegions() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getType() {
        return "cmatch";
    }
}
