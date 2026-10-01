package org.openl.ie.constrainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.openl.ie.constrainer.IntEvent.MAX;
import static org.openl.ie.constrainer.IntEvent.MIN;
import static org.openl.ie.constrainer.IntEvent.VALUE;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class IntBoolVarTest {

    private final Constrainer constrainer = new Constrainer();
    private final IntBoolVar b = constrainer.addIntBoolVar("b");

    @Test
    void notifiesTheObserversAtOnce() throws Failure {
        var events = new ArrayList<IntEvent>();
        b.attachObserver(IntEvent.ALL, events::add);
        var other = constrainer.addIntBoolVar("other");
        other.attachObserver(IntEvent.ALL, events::add);

        b.setTrue();
        other.setFalse();

        assertTrue(b.isTrue());
        assertTrue(other.isFalse());
        assertEquals(List.of(new IntEvent(MIN | VALUE, 1, 1, 0, 1), new IntEvent(MAX | VALUE, 0, 0, 0, 1)), events);
        assertThrows(Failure.class, b::setFalse);
        assertThrows(Failure.class, other::setTrue);
    }

    @Test
    void isUnknownUntilBound() throws Failure {
        assertFalse(b.isTrue());
        assertFalse(b.isFalse());
        assertEquals("b", b.name());
        assertSame(constrainer, b.constrainer());

        b.setTrue();
        b.setTrue();
        assertTrue(b.isTrue());
    }

    @Test
    void restoresTheDomainOnBacktracking() {
        var found = constrainer.solve(() -> () -> {
            b.setFalse();
            return null;
        }, new IntExp[]{b}, () -> null);

        assertTrue(found);
        assertFalse(b.isTrue());
        assertFalse(b.isFalse());
    }

    @Test
    void simplifiesTheExpressionsWithConstants() {
        var t = constrainer.constant(true);
        var f = constrainer.constant(false);

        assertSame(b, t.and(b));
        assertSame(f, f.and(b));
        assertSame(t, t.or(b));
        assertSame(b, f.or(b));
        assertTrue(t.and(true).isTrue());
        assertSame(f, f.and(true));
        assertSame(t, t.or(false));
        assertTrue(f.or(true).isTrue());
        assertTrue(f.or(false).isFalse());

        assertSame(b, b.and(true));
        assertTrue(b.and(false).isFalse());
        assertTrue(b.or(true).isTrue());
        assertSame(b, b.or(false));
    }

    @Test
    void addsConstants() {
        var t = constrainer.constant(true);

        var two = t.add(t);
        assertEquals(2, two.min());
        assertEquals(2, two.max());

        var shifted = t.add(new IntVar(constrainer, 0, 3, "x"));
        assertEquals(1, shifted.min());
        assertEquals(4, shifted.max());
    }

    @Test
    void failsToChangeAConstant() {
        var t = constrainer.constant(true);
        var four = new IntExpConst(constrainer, 4);

        assertThrows(Failure.class, t::setFalse);
        assertThrows(Failure.class, () -> four.setMin(5));
        assertThrows(Failure.class, () -> four.setMax(3));
        assertThrows(Failure.class, () -> four.setValue(5));
    }

    @Test
    void knowsTheComparisonsDecidedFromTheStart() {
        var x = new IntVar(constrainer, 0, 5, "x");

        assertTrue(x.ge(0).isTrue());
        assertTrue(x.gt(5).isFalse());
        assertTrue(x.eq(6).isFalse());
        assertTrue(x.lt(x.add(6)).isTrue());
        assertTrue(constrainer.constant(true).and(x.ge(0)).isTrue());
        assertTrue(x.lt(0).or(x.gt(5)).isFalse());
    }

    @Test
    void makesTheSubjectFollowTheExpression() throws Failure {
        var x = new IntVar(constrainer, 0, 5, "x");
        var y = new IntVar(constrainer, 0, 5, "y");

        x.gt(2).setTrue();
        assertEquals(3, x.min());

        x.lt(y).setTrue();
        assertEquals(4, y.min());
        assertEquals(4, x.max());

        x.eq(4).setFalse();
        assertEquals(3, x.max());
    }
}
