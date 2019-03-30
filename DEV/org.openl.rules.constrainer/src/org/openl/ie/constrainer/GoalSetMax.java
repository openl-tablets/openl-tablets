package org.openl.ie.constrainer;

///////////////////////////////////////////////////////////////////////////////
/*
 * Copyright Exigen Group 1998, 1999, 2000
 * 320 Amboy Ave., Metuchen, NJ, 08840, USA, www.exigengroup.com
 *
 * The copyright to the computer program(s) herein
 * is the property of Exigen Group, USA. All rights reserved.
 * The program(s) may be used and/or copied only with
 * the written permission of Exigen Group
 * or in accordance with the terms and conditions
 * stipulated in the agreement/contract under which
 * the program(s) have been supplied.
 */
///////////////////////////////////////////////////////////////////////////////
/**
 * An implementation of a {@link Goal} that sets the upper bound of a domain of the integer variable.
 */
public class GoalSetMax extends GoalImpl {
    private IntExp _exp;
    private int _max;

    // private UndoableInt _maxI;

    /**
     * Invokes <code>GoalSetMax(exp,exp.max())</code>
     *
     * @param exp The variable of type IntExp
     * @see #GoalSetMax(IntExp,int)
     */
    public GoalSetMax(IntExp exp) {
        this(exp, exp.max());
    }

    /**
     * Creates a goal that is ready to change the upper bound of a domain of it's exp
     *
     * @param exp The variable of type IntExp
     * @param max The variable of type int
     */
    public GoalSetMax(IntExp exp, int max) {
        super(exp.constrainer(), "max");
        _exp = exp;
        _max = max;
        // _maxI = _constrainer.addUndoableInt(max,"max");
    }

    /**
     * Executes a goal.
     *
     * @return Null if the upper limit of a domain was successfully changed
     * @throws Failure
     */
    public Goal execute() throws Failure {
        // Debug.print("\nExecute "+this);
        _exp.setMax(_max);
        // _exp.setMax(_maxI.value());
        return null;
    }

    /**
     * Adjust the value to become the upper limit of a domain of goal's variable.
     */
    public void max(int M) {
        _max = M;
        // _maxI.setValue(M);
    }

    @Override
    public String toString() {
        return _exp + "<=" + _max;
        // return _exp+"<="+_maxI.value();
    }
}
