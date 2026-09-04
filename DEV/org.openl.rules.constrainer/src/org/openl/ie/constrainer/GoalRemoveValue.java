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
 * An implementation of a {@link Goal} that removes the value from the domain of an integer variable.
 */
public class GoalRemoveValue extends GoalImpl {
    private final IntExp _exp;
    private int _value;

    public GoalRemoveValue(IntExp exp) {
        this(exp, exp.max() + 1); // value has to be set later
    }

    public GoalRemoveValue(IntExp exp, int value) {
        super(exp.constrainer(), "remove");
        _exp = exp;
        _value = value;
    }

    @Override
    public Goal execute() throws Failure {
        _exp.removeValue(_value);
        return null;
    }

    @Override
    public String toString() {
        return _exp + "!=" + _value;
    }

    /**
     * Sets the value to be removed from the domain of the variable.
     */
    public void value(int v) {
        _value = v;
    }

} // ~GoalRemoveValue
