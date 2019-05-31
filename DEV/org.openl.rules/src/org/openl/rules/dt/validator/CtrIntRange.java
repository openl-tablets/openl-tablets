package org.openl.rules.dt.validator;

import org.openl.ie.constrainer.IntBoolExp;
import org.openl.ie.constrainer.IntExp;
import org.openl.rules.helpers.IntRange;

public class CtrIntRange extends IntRange {

    public CtrIntRange(long min, long max) {
        super(min, max);
    }

    public IntBoolExp contains(IntExp exp) {
        return exp.ge((int) getMin()).and(exp.le((int) getMax()));
    }

    @Override
    public long getMax() {

        long max = super.getMax();

        if (max >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE - 1;
        }

        return max;
    }

    @Override
    public long getMin() {

        long min = super.getMin();

        if (min <= Integer.MIN_VALUE) {
            return Integer.MIN_VALUE + 1;
        }

        return min;
    }

}
