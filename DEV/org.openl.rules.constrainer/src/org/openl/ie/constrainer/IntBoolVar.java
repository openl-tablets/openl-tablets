package org.openl.ie.constrainer;

import static org.openl.ie.constrainer.IntEvent.MAX;
import static org.openl.ie.constrainer.IntEvent.MIN;
import static org.openl.ie.constrainer.IntEvent.VALUE;

/**
 * A constrained boolean variable: the type of a boolean decision table parameter in the condition formulas.
 * <p>
 * It is also the base of the boolean expressions that keep their own domain. A change of the domain is undone on
 * backtracking. The observers learn about the change at once, without waiting for the propagation.
 */
public class IntBoolVar extends IntBoolExp {

    private static final IntEvent TRUE_EVENT = new IntEvent(MIN | VALUE, 1, 1, 0, 1);
    private static final IntEvent FALSE_EVENT = new IntEvent(MAX | VALUE, 0, 0, 0, 1);

    int min;
    int max = 1;

    IntBoolVar(Constrainer constrainer) {
        super(constrainer);
    }

    IntBoolVar(Constrainer constrainer, String name) {
        super(constrainer, name);
    }

    @Override
    int min() {
        return min;
    }

    @Override
    int max() {
        return max;
    }

    @Override
    void setMin(int value) throws Failure {
        if (value > max) {
            throw new Failure();
        }
        if (value > min) {
            constrainer.addUndo(this::reset);
            min = value;
            notifyObservers(TRUE_EVENT);
        }
    }

    @Override
    void setMax(int value) throws Failure {
        if (value < min) {
            throw new Failure();
        }
        if (value < max) {
            constrainer.addUndo(this::reset);
            max = value;
            notifyObservers(FALSE_EVENT);
        }
    }

    /**
     * Restores the domain [0..1]. A single change binds the variable, so it is the domain before any change.
     */
    private void reset() {
        min = 0;
        max = 1;
    }
}
