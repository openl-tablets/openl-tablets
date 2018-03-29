package org.openl.excel.parser;

import java.util.Objects;

/**
 * Sometimes it's needed to know alignment of a value. For example TBasic tables use this info during parsing the table.
 */
public final class AlignedValue implements ExtendedValue {
    private final Object value;
    private final short indent;

    public AlignedValue(Object value, short indent) {
        this.value = value;
        this.indent = indent;
    }

    @Override
    public Object getValue() {
        return value;
    }

    public short getIndent() {
        return indent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AlignedValue that = (AlignedValue) o;
        return indent == that.indent &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, indent);
    }
}
