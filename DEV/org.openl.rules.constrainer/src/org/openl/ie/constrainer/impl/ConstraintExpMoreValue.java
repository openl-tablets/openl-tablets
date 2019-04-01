package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.*;

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
 * An implementation of the constraint: <code>IntExp > value</code>.
 */
public final class ConstraintExpMoreValue extends ConstraintImpl {
    class ObserverMoreValue extends Observer {
        @Override
        public Object master() {
            return ConstraintExpMoreValue.this;
        }

        @Override
        public int subscriberMask() {
            return EventOfInterest.VALUE | EventOfInterest.MAX;
        }

        @Override
        public String toString() {
            return "ObserverMoreValue";
        }

        @Override
        public void update(Subject exp, EventOfInterest interest) throws Failure {
            // Debug.on();Debug.print("ObserverMoreValue:
            // "+interest);Debug.off();
            IntEvent event = (IntEvent) interest;
            if (event.max() <= _value) {
                exp.constrainer().fail("from ObserverMoreValue");
            }

            _exp.setMin(_value + 1);
        }

    } // ~ ObserverMoreValue
      // PRIVATE MEMBERS

    private IntExp _exp;
    private int _value;

    private Constraint _opposite;

    public ConstraintExpMoreValue(IntExp exp, int value) {
        super(exp.constrainer());

        if (constrainer().showInternalNames()) {
            _name = "(" + exp.name() + ">" + value + ")";
        }

        _exp = exp;
        _value = value;
        _opposite = null;
    }

    @Override
    public Goal execute() throws Failure {
        _exp.setMin(_value + 1); // may fail
        _exp.attachObserver(new ObserverMoreValue());
        return null;
    }

    @Override
    public String toString() {
        return _exp + ">" + _value;
    }

} // eof ConstraintExpMoreValue
