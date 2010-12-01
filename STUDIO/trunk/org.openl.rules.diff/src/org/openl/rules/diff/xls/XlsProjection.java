package org.openl.rules.diff.xls;

import org.openl.rules.diff.hierarchy.AbstractProjection;

public class XlsProjection extends AbstractProjection {
    private Object data;

    public XlsProjection(String name, XlsProjectionType type) {
        super(name, type.name());
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
