package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntExp;
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
 * An implementation of the expression: <code>IntExp<sup>+</sup></code>.
 * The domain of the IntExpPositive has only non-negative part (<code>[o..max]</code>)of
 * the domain of the IntExp.
 */
public final class IntExpPositive extends IntExpImpl {
    class ExpPositiveObserver extends ExpressionObserver {
        @Override
        public Object master() {
            return IntExpPositive.this;
        }

        @Override
        public String toString() {
            return "ExpPositiveObserver: " + _exp;
        }

        @Override
        public void update(Subject subject, EventOfInterest event) throws Failure {
        }

    } // ~ ExpAddValueObserver
    private IntExp _exp;

    private ExpressionObserver _observer;

    public IntExpPositive(IntExp exp) {
        super(exp.constrainer(), "");// exp.name()+"+"+value);
        _exp = exp;
        _observer = new ExpPositiveObserver();
        _exp.attachObserver(_observer);
    }

    @Override
    public boolean contains(int value) {
        return value >= 0 && _exp.contains(value);
    }

    public int max() {
        int max = _exp.max();
        return max >= 0 ? max : MIN_VALUE;
    }

    public int min() {
        int min = _exp.min();
        return min > 0 ? min : 0;
    }

    @Override
    public void onMaskChange() {
        _observer.publish(publisherMask(), _exp);
    }

    @Override
    public void removeValue(int value) throws Failure {
        if (value >= 0) {
            _exp.removeValue(value);
        }
    }

    public void setMax(int max) throws Failure {
        _exp.setMax(max);
    }

    public void setMin(int min) throws Failure {
        if (_exp.min() >= 0) {
            _exp.setMin(min);
        }
    }

    @Override
    public void setValue(int value) throws Failure {
        _exp.setValue(value);
    }

    @Override
    public int size() {
        if (!valid()) {
            return 0;
        }

        return Math.min(_exp.size(), _exp.max() + 1);
    }

    @Override
    public String toString() {
        return (_exp + "++");
    }

    @Override
    public boolean valid() {
        return _exp.min() >= 0;
    }

    @Override
    public int value() throws Failure {
        if (!bound()) {
            constrainer().fail("Attempt to get value of the unbound expression " + this);
        }
        return _exp.value();
    }

} // eof IntExpImpl
