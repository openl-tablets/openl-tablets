package org.openl.rules.cmatch.algorithm;

import org.openl.rules.cmatch.ColumnMatch;
import org.openl.types.IOpenMethod;

public class WColumnMatchTraceObject extends ColumnMatchTraceObject {

    public WColumnMatchTraceObject(ColumnMatch columnMatch, Object[] params) {
        super(columnMatch, params);
    }

    @Override
    public String getDisplayName(int mode) {
        return "WCM " + asString((IOpenMethod) getTraceObject(), mode);
    }

    @Override
    public String getType() {
        return "wcmatch";
    }
}
