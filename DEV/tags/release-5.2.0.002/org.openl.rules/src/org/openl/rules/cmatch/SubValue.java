package org.openl.rules.cmatch;

import org.openl.meta.StringValue;

public class SubValue {
    private StringValue value;
    private int indent;

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
}
