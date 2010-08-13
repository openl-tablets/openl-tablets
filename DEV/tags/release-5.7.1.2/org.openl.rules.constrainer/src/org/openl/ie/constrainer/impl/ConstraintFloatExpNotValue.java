package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constraint;
import org.openl.ie.constrainer.ConstraintImpl;
import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatEvent;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.IntBoolExp;
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
//
//: ConstraintFloatExpNotValue.java
//
/**
 * An implementation of the constraint: <code>FloatExp != value</code>.
 */
public final class ConstraintFloatExpNotValue extends ConstraintImpl {
    // PRIVATE MEMBERS
    private FloatExp _exp;
    private double _value;
    private Constraint _opposite;

    public ConstraintFloatExpNotValue(FloatExp exp, double value) {
        super(exp.constrainer(), "");// exp.name()+"!="+value);
        _exp = exp;
        _value = value;
    }

    public Goal execute() throws Failure {
        class ObserverFloatExpNotValue extends Observer {
            @Override
            public Object master() {
                return ConstraintFloatExpNotValue.this;
            }

            @Override
            public int subscriberMask() {
                return EventOfInterest.VALUE;
            }

            @Override
            public String toString() {
                return "ObserverFloatExpNotValue:" + _exp.name() + "!=" + _value;
            }

            @Override
            public void update(Subject exp, EventOfInterest interest) throws Failure {
                // Debug.print("ObserverFloatExpNotValue("+_exp+") "+interest);
                FloatEvent event = (FloatEvent) interest;
                double value = (event.min() + event.max()) / 2;
                if (FloatCalc.eq(value, _value)) {
                    constrainer().fail("attempt to set a removed value " + value + " for " + exp);
                }
            }

        } // ~ ObserverFloatExpNotValue

        // Debug.print("attach ObserverFloatExpNotValue("+_exp+","+_value+")");
        _exp.attachObserver(new ObserverFloatExpNotValue());

        return null;
    }

    @Override
    public Constraint opposite() {
        if (_opposite == null) {
            _opposite = new ConstraintFloatExpEqualsValue(_exp, _value);
        }
        return _opposite;
    }

    @Override
    public IntBoolExp toIntBoolExp() {
        return _exp.ne(_value);
    }

    @Override
    public String toString() {
        return _exp + "!=" + _value;
    }

} // ~ ConstraintFloatExpNotValue
