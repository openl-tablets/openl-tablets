package org.openl.rules.dt.validator;

import org.openl.rules.helpers.IntRange;

import org.openl.ie.constrainer.IntBoolExp;
import org.openl.ie.constrainer.IntExp;

public class CtrIntRange extends IntRange {

    public CtrIntRange(int min, int max) {
        super(min, max);
    }

    public IntBoolExp contains(IntExp exp) {
        return exp.ge(getMin()).and(exp.le(getMax()));
    }

    @Override
    public int getMax() {
        
        int max = super.getMax();
        
        if (max == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE - 1;
        }
        
        return max;
    }

    @Override
    public int getMin() {
        
        int min = super.getMin();
        
        if (min == Integer.MIN_VALUE) {
            return Integer.MIN_VALUE + 1;
        }
        
        return min;
    }

}
