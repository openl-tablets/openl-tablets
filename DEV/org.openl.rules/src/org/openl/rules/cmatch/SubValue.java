package org.openl.rules.cmatch;

import lombok.Getter;
import lombok.Setter;

import org.openl.meta.StringValue;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.ui.ICellStyle;

public class SubValue {
    private final StringValue value;
    @Getter
    private final int indent;
    @Getter
    @Setter
    private IGridRegion gridRegion;

    public SubValue(StringValue value, ICellStyle cellStyle) {
        this.value = value;
        indent = cellStyle == null ? 0 : cellStyle.getIndent();
    }

    public String getString() {
        return value.getValue();
    }

    public StringValue getStringValue() {
        return value;
    }
}
