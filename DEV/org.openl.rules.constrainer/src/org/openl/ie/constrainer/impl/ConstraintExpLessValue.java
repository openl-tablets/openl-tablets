package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constraint;
import org.openl.ie.constrainer.ConstraintImpl;
import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.Observer;
import org.openl.ie.constrainer.Subject;

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
 * An implementation of the constraint: <code>IntExp < value</code>.
 */
public final class ConstraintExpLessValue extends ConstraintImpl {
    class ObserverLessValue extends Observer {
        @Override
        public Object master() {
            return ConstraintExpLessValue.this;
        }

        @Override
        public int subscriberMask() {
            return EventOfInterest.VALUE | EventOfInterest.MIN;
        }

        @Override
        public String toString() {
            return "ObserverLessValue";
        }

        @Override
        public void update(Subject exp, EventOfInterest interest) throws Failure {
            // Debug.on();Debug.print("ObserverLessValue:
            // "+interest);Debug.off();
            IntEvent event = (IntEvent) interest;
            if (event.min() >= _value) {
                exp.constrainer().fail("from ObserverLessValue");
            }

            _exp.setMax(_value - 1);
        }

    } // ~ ObserverLessValue
    // PRIVATE MEMBERS
    private IntExp _exp;
    private int _value;

    private Constraint _opposite;

    public ConstraintExpLessValue(IntExp exp, int value) {
        super(exp.constrainer());
        _exp = exp;
        _value = value;
        _opposite = null;

        if (constrainer().showInternalNames()) {
            _name = "(" + exp.name() + "<" + value + ")";
        }
    }

    public Goal execute() throws Failure {
        _exp.setMax(_value - 1); // may fail
        _exp.attachObserver(new ObserverLessValue());
        return null;
    }

    /*
     * resetValue(int v) throws Failure { _value = v; _exp.setMax(_value-1); }
     */
    public void resetValue(int v) {
        _value = v;
    }

    @Override
    public String toString() {
        return _exp + "<" + _value;
    }

} // ~ ConstraintExpLessValue
