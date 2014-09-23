package org.openl.rules.cmatch.algorithm;

import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.table.IGridRegion;
import org.openl.types.IOpenMethod;

import java.util.List;

public class WColumnMatchTraceObject extends ATableTracerNode {

    public WColumnMatchTraceObject(ColumnMatch columnMatch, Object[] params) {
        super(columnMatch, params);
    }

    @Override
    public String getDisplayName(int mode) {
        return "WCM " + asString((IOpenMethod) getTraceObject(), mode);
    }

    public List<IGridRegion> getGridRegions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getType() {
        return "wcmatch";
    }
}
