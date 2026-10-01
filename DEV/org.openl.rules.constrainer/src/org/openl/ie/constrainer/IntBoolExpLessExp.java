package org.openl.ie.constrainer;

import static org.openl.ie.constrainer.IntEvent.MAX;
import static org.openl.ie.constrainer.IntEvent.MIN;

/**
 * The expression {@code left < right}.
 */
final class IntBoolExpLessExp extends IntBoolExpForSubject {

    private final IntExp left;
    private final IntExp right;

    IntBoolExpLessExp(IntExp left, IntExp right) {
        super(left.constrainer);
        this.left = left;
        this.right = right;
        initDomain();
        Observer observer = event -> update();
        left.attachObserver(MIN | MAX, observer);
        right.attachObserver(MIN | MAX, observer);
    }

    @Override
    boolean isSubjectTrue() {
        return left.max() < right.min();
    }

    @Override
    boolean isSubjectFalse() {
        return left.min() >= right.max();
    }

    @Override
    void setSubjectTrue() throws Failure {
        left.setMax(right.max() - 1);
        right.setMin(left.min() + 1);
    }

    @Override
    void setSubjectFalse() throws Failure {
        left.setMin(right.min());
        right.setMax(left.max());
    }
}
