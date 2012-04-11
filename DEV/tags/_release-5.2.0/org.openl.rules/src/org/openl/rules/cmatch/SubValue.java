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
        this.indent = (cellStyle == null) ? 0 : cellStyle.getIdent();
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
