package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.ConstraintImpl;
import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatEvent;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.Goal;
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
 * An implementation of the constraint:
 * <code>FloatExp1 == FloatExp2 + offset</code>.
 */
public final class ConstraintFloatExpEqualsExp extends ConstraintImpl {
    class ObserverFloatEqual extends Observer {
        @Override
        public Object master() {
            return ConstraintFloatExpEqualsExp.this;
        }

        @Override
        public int subscriberMask() {
            return EventOfInterest.MINMAX;// | EventOfInterest.REMOVE;
        }

        @Override
        public String toString() {
            return _name + "(ObserverFloatEqual)";
        }

        @Override
        public void update(Subject exp, EventOfInterest interest) throws Failure {
            // Debug.on();Debug.print("ObserverFloatEqual: "+_name+" Event:
            // "+interest);Debug.off();
            FloatEvent event = (FloatEvent) interest;
            // if (event.isRemoveEvent())
            // {
            // int max = event.numberOfRemoves();
            // for(int i =0; i < max; ++i)
            // {
            // if (exp == _exp1)
            // _exp2.removeValue(event.removed(i)-_offset);
            // else
            // if (exp == _exp2)
            // _exp1.removeValue(event.removed(i)+_offset);
            // }
            // }
            // else
            {
                minmax();
            }
        }

    } // ~ ObserverFloatEqual
    // PRIVATE MEMBERS
    private FloatExp _exp1;
    private FloatExp _exp2;

    private double _offset;

    public ConstraintFloatExpEqualsExp(FloatExp exp1, FloatExp exp2) {
        this(exp1, exp2, 0d);
    }

    /**
     * exp1 == exp2 + offset
     */
    public ConstraintFloatExpEqualsExp(FloatExp exp1, FloatExp exp2, double offset) {
        super(exp1.constrainer());
        _exp1 = exp1;
        _exp2 = exp2;
        _offset = offset;

        if (constrainer().showInternalNames()) {
            if (offset == 0) {
                _name = "(" + exp1.name() + "=" + exp2.name() + ")";
            } else if (offset > 0) {
                _name = "(" + exp1.name() + "=" + exp2.name() + "+" + offset + ")";
            } else {
                _name = "(" + exp1.name() + "=" + exp2.name() + offset + ")";
            }
        }
    }

    public Goal execute() throws Failure {
        minmax(); // may fail
        ObserverFloatEqual observer = new ObserverFloatEqual();
        _exp1.attachObserver(observer);
        _exp2.attachObserver(observer);
        return null;
    }

    public void minmax() throws Failure {
        _exp1.setMax(_exp2.max() + _offset); // may fail
        _exp2.setMax(_exp1.max() - _offset); // may fail
        _exp1.setMin(_exp2.min() + _offset); // may fail
        _exp2.setMin(_exp1.min() - _offset); // may fail
    }

    @Override
    public String toString() {
        return _exp1 + "=" + _exp2;
    }

} // ~ ConstraintFloatExpEqualsExp

