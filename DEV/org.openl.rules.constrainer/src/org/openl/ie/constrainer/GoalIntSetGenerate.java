package org.openl.ie.constrainer;

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2002
 * </p>
 * <p>
 * Company:
 * </p>
 *
 * @author unascribed
 * @version 1.0
 */

public class GoalIntSetGenerate extends GoalImpl {
    IntSetVarArray _array;

    public GoalIntSetGenerate(IntSetVarArray array) {
        super(array.constrainer());
        _array = array;

    }

    public Goal execute() throws Failure {
        Goal goal;
        if (_array.size() == 1) {
            goal = _array.get(0).generate();
        } else {
            goal = new GoalAnd(_array.get(0).generate(), _array.get(1).generate());
            for (int i = 2; i < _array.size(); i++) {
                goal = new GoalAnd(goal, _array.get(i).generate());
            }
        }
        return goal;
    }
}