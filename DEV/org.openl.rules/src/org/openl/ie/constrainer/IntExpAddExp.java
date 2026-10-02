package org.openl.ie.constrainer;

import static org.openl.ie.constrainer.IntEvent.MAX;
import static org.openl.ie.constrainer.IntEvent.MIN;
import static org.openl.ie.constrainer.IntEvent.VALUE;

/**
 * The expression {@code exp1 + exp2}.
 * <p>
 * A variable keeps the bounds of the sum, and the observers of the expression observe that variable.
 */
final class IntExpAddExp extends IntExp {

    private final IntExp exp1;
    private final IntExp exp2;
    private final IntVar sum;

    IntExpAddExp(IntExp exp1, IntExp exp2) {
        super(exp1.constrainer);
        this.exp1 = exp1;
        this.exp2 = exp2;
        sum = new IntVar(constrainer, sumMin(), sumMax(), "");
        Observer observer = event -> {
            sum.setMin(sumMin());
            sum.setMax(sumMax());
        };
        exp1.attachObserver(MIN | MAX | VALUE, observer);
        exp2.attachObserver(MIN | MAX | VALUE, observer);
    }

    private int sumMin() {
        return exp1.min() + exp2.min();
    }

    private int sumMax() {
        return exp1.max() + exp2.max();
    }

    @Override
    void attachObserver(int mask, Observer observer) {
        sum.attachObserver(mask, observer);
    }

    @Override
    int min() {
        return sum.min();
    }

    @Override
    int max() {
        return sum.max();
    }

    @Override
    void setMin(int min) throws Failure {
        if (min <= sum.min()) {
            return;
        }
        var min1 = min - exp2.max();
        if (min1 > exp1.min()) {
            exp1.setMin(min1);
        }
        var min2 = min - exp1.max();
        if (min2 > exp2.min()) {
            exp2.setMin(min2);
        }
    }

    @Override
    void setMax(int max) throws Failure {
        if (max >= sum.max()) {
            return;
        }
        var max1 = max - exp2.min();
        if (max1 < exp1.max()) {
            exp1.setMax(max1);
        }
        var max2 = max - exp1.min();
        if (max2 < exp2.max()) {
            exp2.setMax(max2);
        }
    }
}
