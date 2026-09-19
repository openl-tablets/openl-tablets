package org.openl.ie.constrainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class ChoicePointLabelTest {

    private final Constrainer constrainer = new Constrainer("test");

    @Test
    void labelsOfTheSameConstrainerWithTheSameNumberAreEqual() {
        var one = new ChoicePointLabel(constrainer, 1);
        var other = new ChoicePointLabel(constrainer, 1);

        assertEquals(one, other);
        assertEquals(one.hashCode(), other.hashCode());
    }

    @Test
    void labelsDifferingInNumberOrConstrainerAreNotEqual() {
        var label = new ChoicePointLabel(constrainer, 1);

        assertNotEquals(label, new ChoicePointLabel(constrainer, 2));
        assertNotEquals(label, new ChoicePointLabel(new Constrainer("other"), 1));
        assertNotEquals(label, new Object());
    }
}
