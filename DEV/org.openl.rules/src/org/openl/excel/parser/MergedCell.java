package org.openl.excel.parser;

public enum MergedCell implements ExtendedValue {
    MERGE_WITH_LEFT,
    MERGE_WITH_UP;

    @Override
    public Object getValue() {
        return this;
    }
}
