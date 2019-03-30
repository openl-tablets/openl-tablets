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
 * An implementation of a {@link Goal} that sets the minimal value of the integer variable.
 */
public class GoalSetMin extends GoalImpl {
    private IntExp _exp;
    private int _min;

    // private UndoableInt _minI;

    public GoalSetMin(IntExp exp) {
        this(exp, exp.min());
    }

    public GoalSetMin(IntExp exp, int min) {
        super(exp.constrainer(), "min");
        _exp = exp;
        _min = min;
        // _minI = _constrainer.addUndoableInt(min,"min");
    }

    public Goal execute() throws Failure {
        // Debug.print("\nExecute "+this);
        _exp.setMin(_min);
        // _exp.setMin(_minI.value());
        return null;
    }

    /**
     * Sets the minimal value to be set in the variable.
     */
    public void min(int m) {
        _min = m;
        // _minI.setValue(m);
    }

    @Override
    public String toString() {
        return _exp + ">=" + _min;
        // return _exp+">="+_minI.value();
    }

}
