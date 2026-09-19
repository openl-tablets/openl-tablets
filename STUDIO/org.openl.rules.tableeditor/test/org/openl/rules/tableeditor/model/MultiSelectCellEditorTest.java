package org.openl.rules.tableeditor.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import org.openl.rules.tableeditor.model.MultiSelectCellEditor.MultiChoiceParam;

class MultiSelectCellEditorTest {

    private static MultiChoiceParam param(String[] choices, String[] displayValues, String separator) {
        return new MultiChoiceParam(choices, displayValues, separator, "\\");
    }

    @Test
    void paramsHoldingEqualChoicesAreEqual() {
        var one = param(new String[]{"a", "b"}, new String[]{"A", "B"}, ",");
        var other = param(new String[]{"a", "b"}, new String[]{"A", "B"}, ",");

        assertEquals(one, other);
        assertEquals(one.hashCode(), other.hashCode());
    }

    @Test
    void paramsDifferingInAnyComponentAreNotEqual() {
        var param = param(new String[]{"a", "b"}, new String[]{"A", "B"}, ",");

        assertNotEquals(param, param(new String[]{"a", "c"}, new String[]{"A", "B"}, ","));
        assertNotEquals(param, param(new String[]{"a", "b"}, new String[]{"A", "C"}, ","));
        assertNotEquals(param, param(new String[]{"a", "b"}, new String[]{"A", "B"}, ";"));
        assertNotEquals(param, new MultiChoiceParam(new String[]{"a", "b"}, new String[]{"A", "B"}, ",", "/"));
        assertNotEquals(param, new Object());
    }

    @Test
    void toStringShowsTheContentOfBothArrays() {
        var param = param(new String[]{"a", "b"}, new String[]{"A", "B"}, ",");

        assertEquals("MultiChoiceParam[choices=[a, b], displayValues=[A, B], separator=,, separatorEscaper=\\]",
                param.toString());
    }
}
