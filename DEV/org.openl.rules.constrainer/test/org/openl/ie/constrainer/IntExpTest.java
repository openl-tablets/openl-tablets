package org.openl.ie.constrainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IntExpTest {

    private final Constrainer constrainer = new Constrainer();
    private final IntVar x = new IntVar(constrainer, 0, 5, "x");
    private final IntVar y = new IntVar(constrainer, 0, 5, "y");
    private final IntVar z = new IntVar(constrainer, 0, 5, "z");

    @Test
    void shiftedExpressionNarrowsTheOriginalOne() throws Failure {
        var shifted = x.add(2);

        shifted.setMax(4);
        shifted.setMin(3);
        assertEquals(1, x.min());
        assertEquals(2, x.max());
        assertEquals(3, shifted.min());
        assertEquals(4, shifted.max());

        shifted.removeValue(4);
        assertTrue(x.bound());
        assertEquals(1, x.min());
    }

    @Test
    void sumNarrowsItsTerms() throws Failure {
        var sum = IntExpAddArray.sum(constrainer, new IntExp[]{x, y, z});
        assertEquals(0, sum.min());
        assertEquals(15, sum.max());

        sum.setMin(0);
        sum.setMax(15);
        assertEquals(0, sum.min());
        assertEquals(15, sum.max());

        sum.removeValue(15);
        assertEquals(14, sum.max());

        sum.setMax(3);
        assertEquals(3, sum.max());
        assertEquals(3, x.max());
        assertEquals(3, y.max());
        assertEquals(3, z.max());
    }

    @Test
    void disjunctionMakesTheUnknownOperandTrue() throws Failure {
        x.setMin(1);

        x.eq(0).or(y.eq(3)).setTrue();

        assertTrue(y.bound());
        assertEquals(3, y.min());
    }

    @Test
    void conjunctionMakesTheUnknownOperandFalse() throws Failure {
        x.ge(0).and(y.eq(3)).setFalse();

        assertFalse(y.contains(3));
        assertEquals(0, y.min());
        assertEquals(5, y.max());
    }
}
