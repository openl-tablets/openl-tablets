package org.openl.ie.constrainer;

import static org.openl.ie.constrainer.IntEvent.MAX;
import static org.openl.ie.constrainer.IntEvent.MIN;

/**
 * The expression {@code exp1 || exp2}.
 */
final class IntBoolExpOr extends IntBoolExpForSubject {

    private final IntBoolExp exp1;
    private final IntBoolExp exp2;

    IntBoolExpOr(IntBoolExp exp1, IntBoolExp exp2) {
        super(exp1.constrainer);
        this.exp1 = exp1;
        this.exp2 = exp2;
        initDomain();
        exp1.attachObserver(MIN | MAX, event -> follow(event, exp2));
        exp2.attachObserver(MIN | MAX, event -> follow(event, exp1));
    }

    /**
     * Follows the change of an operand: a true operand makes the expression true, and a false operand makes the
     * expression equal to the other operand.
     */
    private void follow(IntEvent event, IntBoolExp other) throws Failure {
        if (event.isMinEvent()) {
            setDomainMin(1);
        } else {
            setDomainMin(other.min());
            setDomainMax(other.max());
            other.setMin(min);
            other.setMax(max);
        }
    }

    @Override
    boolean isSubjectTrue() {
        return exp1.isTrue() || exp2.isTrue();
    }

    @Override
    boolean isSubjectFalse() {
        return exp1.isFalse() && exp2.isFalse();
    }

    @Override
    void setSubjectTrue() throws Failure {
        if (exp1.isFalse()) {
            exp2.setTrue();
        }
        if (exp2.isFalse()) {
            exp1.setTrue();
        }
    }

    @Override
    void setSubjectFalse() throws Failure {
        exp1.setFalse();
        exp2.setFalse();
    }
}
