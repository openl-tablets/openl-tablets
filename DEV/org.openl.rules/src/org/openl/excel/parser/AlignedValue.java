package org.openl.excel.parser;

import java.util.Objects;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Sometimes it's needed to know alignment of a value. For example TBasic tables use this info during parsing the table.
 */
@RequiredArgsConstructor
public final class AlignedValue implements ExtendedValue {
    @Getter
    private final Object value;
    @Getter
    private final short indent;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (AlignedValue) o;
        return indent == that.indent && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, indent);
    }
}
