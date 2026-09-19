package org.openl.rules.dt.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CtrIntRangeTest {

    @Test
    void boundsWiderThanAnIntAreBroughtBackInsideIt() {
        var range = new CtrIntRange(Long.MIN_VALUE, Long.MAX_VALUE);

        assertEquals(Integer.MIN_VALUE + 1L, range.getMin());
        assertEquals(Integer.MAX_VALUE - 1L, range.getMax());
    }

    @Test
    void boundsThatAlreadyFitAreLeftAlone() {
        var range = new CtrIntRange(-5, 7);

        assertEquals(-5L, range.getMin());
        assertEquals(7L, range.getMax());
    }
}
