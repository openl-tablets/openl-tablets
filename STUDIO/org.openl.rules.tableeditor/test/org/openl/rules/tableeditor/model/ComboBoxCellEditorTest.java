package org.openl.rules.tableeditor.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import org.openl.rules.tableeditor.model.ComboBoxCellEditor.ComboBoxParam;

class ComboBoxCellEditorTest {

    @Test
    void paramsHoldingEqualChoicesAreEqual() {
        var one = new ComboBoxParam(new String[]{"a", "b"}, new String[]{"A", "B"});
        var other = new ComboBoxParam(new String[]{"a", "b"}, new String[]{"A", "B"});

        assertEquals(one, other);
        assertEquals(one.hashCode(), other.hashCode());
    }

    @Test
    void paramsDifferingInAnyArrayAreNotEqual() {
        var param = new ComboBoxParam(new String[]{"a", "b"}, new String[]{"A", "B"});

        assertNotEquals(param, new ComboBoxParam(new String[]{"a", "c"}, new String[]{"A", "B"}));
        assertNotEquals(param, new ComboBoxParam(new String[]{"a", "b"}, new String[]{"A", "C"}));
        assertNotEquals(param, new Object());
    }

    @Test
    void toStringShowsTheContentOfBothArrays() {
        var param = new ComboBoxParam(new String[]{"a", "b"}, new String[]{"A", "B"});

        assertEquals("ComboBoxParam[choices=[a, b], displayValues=[A, B]]", param.toString());
    }
}
