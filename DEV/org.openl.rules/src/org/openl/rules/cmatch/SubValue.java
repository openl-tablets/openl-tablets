package org.openl.rules.cmatch;

import org.openl.meta.StringValue;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.ui.ICellStyle;

public class SubValue {
    private StringValue value;
    private int indent;
    private IGridRegion gridRegion;

    public SubValue(StringValue value, ICellStyle cellStyle) {
        this.value = value;
        indent = (cellStyle == null) ? 0 : cellStyle.getIndent();
    }

    public IGridRegion getGridRegion() {
        return gridRegion;
    }

    public int getIndent() {
        return indent;
    }

    public String getString() {
        return value.getValue();
    }

    public StringValue getStringValue() {
        return value;
    }

    public void setGridRegion(IGridRegion gridRegion) {
        this.gridRegion = gridRegion;
    }
}
