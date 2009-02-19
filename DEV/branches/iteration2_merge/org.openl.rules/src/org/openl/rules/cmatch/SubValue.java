package org.openl.rules.cmatch;

import org.openl.meta.StringValue;
import org.openl.rules.table.IGridRegion;

public class SubValue {
    private StringValue value;
    private int indent;
    private IGridRegion gridRegion;

    public SubValue(StringValue value, int indent) {
        this.indent = indent;
        this.value = value;
    }

    public int getIndent() {
        return indent;
    }

    public StringValue getStringValue() {
        return value;
    }

    public String getString() {
        return value.getValue();
    }

    public IGridRegion getGridRegion() {
        return gridRegion;
    }

    public void setGridRegion(IGridRegion gridRegion) {
        this.gridRegion = gridRegion;
    }
}
