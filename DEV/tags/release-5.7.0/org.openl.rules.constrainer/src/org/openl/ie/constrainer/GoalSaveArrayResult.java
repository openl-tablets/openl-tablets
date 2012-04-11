package org.openl.ie.constrainer;

import java.util.ArrayList;
import java.util.List;

public class GoalSaveArrayResult extends GoalImpl {
    IntExpArray ary;

    List<int[]> result = new ArrayList<int[]>();

    public GoalSaveArrayResult(Constrainer c, IntExpArray ary) {
        super(c);
        this.ary = ary;
    }

    public Goal execute() throws Failure {

        int[] res = new int[ary.size()];

        for (int i = 0; i < res.length; i++) {
            res[i] = ary.elementAt(i).value();
        }

        result.add(res);
        return null;
    }

    /**
     * @return Returns the result.
     */
    public int[] getFirstResult() {
        // if (result.size() == 0)
        // {
        // return new int[ary.size()];
        // }

        return result.get(0);
    }

    public List<int[]> getResult() {
        return result;
    }

}
