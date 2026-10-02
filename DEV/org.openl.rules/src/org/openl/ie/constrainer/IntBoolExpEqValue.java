package org.openl.ie.constrainer;

import static org.openl.ie.constrainer.IntEvent.ALL;

/**
 * The expression {@code exp == value}.
 */
final class IntBoolExpEqValue extends IntBoolExpForSubject {

    private final IntExp exp;
    private final int value;

    IntBoolExpEqValue(IntExp exp, int value) {
        super(exp.constrainer);
        this.exp = exp;
        this.value = value;
        initDomain();
        exp.attachObserver(ALL, event -> update());
    }

    @Override
    boolean isSubjectTrue() {
        return exp.min() == value && exp.max() == value;
    }

    @Override
    boolean isSubjectFalse() {
        return !exp.contains(value);
    }

    @Override
    void setSubjectTrue() throws Failure {
        exp.setValue(value);
    }

    @Override
    void setSubjectFalse() throws Failure {
        exp.removeValue(value);
    }
}
