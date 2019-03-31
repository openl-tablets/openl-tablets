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
 * An implementation of a {@link Goal} that tries to set up the value of the integer variable. If it succeededs then the
 * variable becomes "bound"
 */
public class GoalSetValue extends GoalImpl {
    private IntExp _exp;
    private int _value;

    /**
     * It invokes <code>GoalSetValue(exp,exp.min() -1)</code>
     *
     * @param exp An IntExp type variable to be set up.
     */
    public GoalSetValue(IntExp exp) {
        this(exp, exp.min() - 1); // value has to be set later
    }

    // private UndoableInt _valueI;
    /**
     * Creates the goal that is ready to make an IntExp type variable bound by setting it up.
     *
     * @param exp An IntExp type variable to be set up.
     * @param value The integer value to be assigned to "exp".
     */
    public GoalSetValue(IntExp exp, int value) {
        super(exp.constrainer(), "set");
        _exp = exp;
        _value = value;
        // _valueI = _constrainer.addUndoableInt(value,"set");
    }

    /**
     * Executes a goal
     *
     * @return Null if succeeded.
     * @throws Failure if such a value is out of domain or doesn't satisfy to some of the constraints associated with a
     *             given IntExp variable and so can't be assigned to it.
     */
    @Override
    public Goal execute() throws Failure {
        // Debug.print("\nExecute "+this);
        _exp.setValue(_value);
        // _exp.setValue(_valueI.value());
        return null;
    }

    @Override
    public String toString() {
        return _exp + "=" + _value;
        // return _exp+"="+_valueI.value();
    }

    /**
     * Sets the internal value to be equal to it's argument
     *
     * @param v An integer value
     */
    public void value(int v) {
        _value = v;
        // _valueI.setValue(v);
    }

} // ~GoalSetValue
