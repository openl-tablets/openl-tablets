package org.openl.ie.constrainer;

import static org.openl.ie.constrainer.IntEvent.ALL;

/**
 * The expression {@code exp + delta}. Its domain is the domain of {@code exp} shifted by the delta.
 */
final class IntExpAddValue extends IntExp {

    private final IntExp exp;
    private final int delta;

    IntExpAddValue(IntExp exp, int delta) {
        super(exp.constrainer);
        this.exp = exp;
        this.delta = delta;
        exp.attachObserver(ALL, event -> notifyObservers(event.shift(delta)));
    }

    @Override
    public IntExp add(int value) {
        return exp.add(delta + value);
    }

    @Override
    int min() {
        return exp.min() + delta;
    }

    @Override
    int max() {
        return exp.max() + delta;
    }

    @Override
    boolean contains(int value) {
        return exp.contains(value - delta);
    }

    @Override
    void setMin(int min) throws Failure {
        exp.setMin(min - delta);
    }

    @Override
    void setMax(int max) throws Failure {
        exp.setMax(max - delta);
    }

    @Override
    void setValue(int value) throws Failure {
        exp.setValue(value - delta);
    }

    @Override
    void removeValue(int value) throws Failure {
        exp.removeValue(value - delta);
    }
}
