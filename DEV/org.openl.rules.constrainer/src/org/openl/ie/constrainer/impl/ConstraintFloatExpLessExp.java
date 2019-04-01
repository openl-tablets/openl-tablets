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
//
//: ConstraintFloatExpLessExp.java
//
/**
 * An implementation of the constraint: <code>FloatExp1 <= FloatExp2 + value</code>.
 */
public final class ConstraintFloatExpLessExp extends ConstraintImpl {
    class ObserverExp1Min extends Observer {
        @Override
        public Object master() {
            return ConstraintFloatExpLessExp.this;
        }

        @Override
        public int subscriberMask() {
            return EventOfInterest.MIN;
        }

        @Override
        public String toString() {
            return _name + "(min:" + _exp1.name() + ")";
        }

        @Override
        public void update(Subject exp, EventOfInterest interest) throws Failure {
            // Debug.print("ObserverExp1Min("+_exp1+","+_exp2+") "+interest);
            _exp2.setMin(_exp1.min() + _offset); // may fail
        }

    } // ~ ObserverExp1Min

    class ObserverExp2Max extends Observer {
        @Override
        public Object master() {
            return ConstraintFloatExpLessExp.this;
        }

        @Override
        public int subscriberMask() {
            return EventOfInterest.MAX;
        }

        @Override
        public String toString() {
            return _name + "(max:" + _exp2.name() + ")";
        }

        @Override
        public void update(Subject exp, EventOfInterest interest) throws Failure {
            // Debug.print("ObserverExp2Max("+_exp1+","+_exp2+") "+interest);
            _exp1.setMax(_exp2.max() - _offset); // may fail
        }

    } // ~ ObserverExp2Max

    // PRIVATE MEMBERS
    private FloatExp _exp1;
    private FloatExp _exp2;
    private double _offset;
    private Constraint _opposite;

    public ConstraintFloatExpLessExp(FloatExp exp1, FloatExp exp2) {
        this(exp1, exp2, 0);
    }

    /**
     * exp1 <= exp2 + offset
     */
    public ConstraintFloatExpLessExp(FloatExp exp1, FloatExp exp2, double offset) {
        super(exp1.constrainer(), "");// exp1.name()+"<="+exp2.name()+"+"+offset);
        _name = "";
        if (constrainer().showInternalNames()) {
            _name = exp1.name() + "<=" + exp2.name() + "+" + offset;
        }
        _exp1 = exp1;
        _exp2 = exp2;
        _offset = offset;
        _opposite = null;
    }

    @Override
    public Goal execute() throws Failure {
        _exp1.attachObserver(new ObserverExp1Min());
        _exp2.attachObserver(new ObserverExp2Max());

        _exp1.setMax(_exp2.max() - _offset); // may fail
        _exp2.setMin(_exp1.min() + _offset); // may fail
        return null;
    }

} // ~ ConstraintFloatExpLessExp
