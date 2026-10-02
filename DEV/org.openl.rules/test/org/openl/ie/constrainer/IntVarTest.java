package org.openl.ie.constrainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.openl.ie.constrainer.IntEvent.MAX;
import static org.openl.ie.constrainer.IntEvent.MIN;
import static org.openl.ie.constrainer.IntEvent.REMOVE;
import static org.openl.ie.constrainer.IntEvent.VALUE;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class IntVarTest {

    private final Constrainer constrainer = new Constrainer();

    @Test
    void smallDomainLosesAnyValue() throws Failure {
        var x = new IntVar(constrainer, 0, 5, "x");

        x.removeValue(2);
        assertFalse(x.contains(2));
        assertTrue(x.contains(3));
        assertEquals(0, x.min());
        assertEquals(5, x.max());

        x.removeValue(0);
        assertEquals(1, x.min());
        x.removeValue(1);
        assertEquals(3, x.min());

        x.removeValue(4);
        x.setMax(4);
        assertEquals(3, x.max());
        assertTrue(x.bound());
    }

    @Test
    void largeDomainLosesValuesOnlyFromItsEnds() throws Failure {
        var x = new IntVar(constrainer, 0, 200, "x");

        x.removeValue(100);
        assertTrue(x.contains(100));

        x.removeValue(0);
        x.removeValue(200);
        assertEquals(1, x.min());
        assertEquals(199, x.max());
        assertFalse(x.contains(0));
        assertFalse(x.contains(200));
    }

    @Test
    void domainSpansAllIntegers() throws Failure {
        var x = new IntVar(constrainer, Integer.MIN_VALUE, Integer.MAX_VALUE, "x");
        assertTrue(x.contains(Integer.MIN_VALUE));
        assertTrue(x.contains(Integer.MAX_VALUE));

        x.setValue(0);
        assertTrue(x.bound());
        assertEquals(0, x.min());
    }

    @Test
    void failsWhenTheDomainBecomesEmpty() throws Failure {
        var x = new IntVar(constrainer, 0, 5, "x");

        assertThrows(Failure.class, () -> x.setMin(6));
        assertThrows(Failure.class, () -> x.setMax(-1));

        x.removeValue(3);
        assertThrows(Failure.class, () -> x.setValue(3));

        x.setValue(4);
        assertThrows(Failure.class, () -> x.removeValue(4));
    }

    @Test
    void ignoresTheValuesOutsideTheDomain() throws Failure {
        var x = new IntVar(constrainer, 0, 5, "x");

        x.setMin(-1);
        x.setMax(6);
        x.removeValue(7);
        x.removeValue(2);
        x.removeValue(2);

        assertEquals(0, x.min());
        assertEquals(5, x.max());
        assertFalse(x.contains(-1));
        assertFalse(x.contains(6));
    }

    @Test
    void notifiesTheObserversAboutTheChangesSinceTheLastPropagation() throws Failure {
        var x = new IntVar(constrainer, 0, 10, "x");
        var events = new ArrayList<IntEvent>();
        x.attachObserver(IntEvent.ALL, events::add);

        x.setMin(3);
        x.setMax(7);
        x.propagate();
        x.removeValue(5);
        x.propagate();
        x.setValue(4);
        x.propagate();

        assertEquals(List.of(new IntEvent(MIN | MAX, 3, 7, 0, 10),
                new IntEvent(REMOVE, 3, 7, 3, 7),
                new IntEvent(VALUE | MIN | MAX, 4, 4, 3, 7)), events);
    }

    @Test
    void notifiesOnlyTheObserversOfTheKindOfTheChange() throws Failure {
        var x = new IntVar(constrainer, 0, 10, "x");
        var events = new ArrayList<IntEvent>();
        x.attachObserver(MAX, events::add);

        x.setMin(3);
        x.propagate();
        x.setMax(7);
        x.propagate();

        assertEquals(List.of(new IntEvent(MAX, 3, 7, 3, 10)), events);
    }

    @Test
    void restoresTheDomainOnBacktracking() {
        var x = new IntVar(constrainer, 0, 5, "x");
        var events = new ArrayList<IntEvent>();

        var found = constrainer.solve(() -> () -> {
            x.attachObserver(IntEvent.ALL, events::add);
            x.removeValue(2);
            x.setMin(1);
            return null;
        }, new IntExp[]{x}, () -> null);

        assertTrue(found);
        assertEquals(List.of(new IntEvent(REMOVE | MIN, 1, 5, 0, 5), new IntEvent(MAX | VALUE, 1, 1, 1, 5)),
                events);
        assertTrue(x.contains(2));
        assertEquals(0, x.min());
        assertEquals(5, x.max());
    }
}
