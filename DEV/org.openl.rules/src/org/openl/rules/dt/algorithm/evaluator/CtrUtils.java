package org.openl.rules.dt.algorithm.evaluator;

import org.openl.ie.constrainer.IntBoolExp;
import org.openl.ie.constrainer.IntExp;

/**
 * Used inside OpenL rules.
 */
public final class CtrUtils {

    private CtrUtils() {
    }

    public static IntBoolExp containsCtr(int[] ary, IntExp exp) {

        if (ary == null || ary.length == 0) {
            return exp.constrainer().constant(false);
        }

        var b = exp.eq(ary[0]);

        for (var i = 1; i < ary.length; i++) {
            b = b.or(exp.eq(ary[i]));
        }

        return b;

    }

}
