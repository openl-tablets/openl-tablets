package org.openl.rules.dt.algorithm.evaluator;

import org.openl.ie.constrainer.IntBoolExp;
import org.openl.ie.constrainer.IntBoolExpConst;
import org.openl.ie.constrainer.IntExp;

/**
 * Used inside OpenL rules.
 */
public class CtrUtils {

    public static IntBoolExp containsCtr(int[] ary, IntExp exp) {

        if (ary == null || ary.length == 0)
            return IntBoolExpConst.getIntBoolExpConst(exp.constrainer(), false);

        IntBoolExp b = exp.eq(ary[0]);

        for (int i = 1; i < ary.length; i++) {
            b = b.or(exp.eq(ary[i]));
        }

        return b;

    }

    public static IntBoolExp containsCtr(Integer[] ary, IntExp exp) {
        IntBoolExp b = IntBoolExpConst.getIntBoolExpConst(exp.constrainer(), true);

        for (Integer integer : ary) {
            b = b.or(exp.eq(integer));
        }

        return b;
    }

}
