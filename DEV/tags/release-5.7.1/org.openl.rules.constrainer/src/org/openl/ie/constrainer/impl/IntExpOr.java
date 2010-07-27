package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Expression;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntExp;

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
//: IntExpOr.java
//
/**
 * An implementation of the expression: <code>(IntExp1 || IntExp2)</code>.
 * The domain of the IntExpOr is the union of the domains of the IntExp1 and
 * IntExp2.
 */
public final class IntExpOr extends IntExpImpl {
    class ExpOrExpObserver extends ExpressionObserver {
        private IntExp _second;

        ExpOrExpObserver(Expression exp) {
            super();
            if (exp == _exp1) {
                _second = _exp2;
            } else {
                _second = _exp1;
            }
        }

        /*
         *
         * public void transformValueEvent(EventOfInterest event) throws Failure {
         * IntEvent e = (IntEvent) event; if (_second.size() <= 0) // yes, it is
         * possible!!!! { EventOfInterest exp_event =
         * createIntValueEvent(e.value()); notifyObservers(exp_event); } }
         *
         * public void transformMinEvent(EventOfInterest event) throws Failure {
         * IntEvent e = (IntEvent) event; EventOfInterest exp_event; if
         * (_second.min() > e.value()) exp_event = createIntMinEvent(e.value());
         * else exp_event = createIntMinEvent(_second.min());
         * notifyObservers(exp_event); }
         *
         * public void transformMaxEvent(EventOfInterest event) throws Failure {
         * IntEvent e = (IntEvent) event; EventOfInterest exp_event; if
         * (_second.max() < e.value()) exp_event = createIntMaxEvent(e.value());
         * else exp_event = createIntMaxEvent(_second.max());
         * notifyObservers(exp_event); }
         *
         * public void transformRemoveEvent(EventOfInterest event) throws
         * Failure { IntEvent e = (IntEvent) event; if
         * (!_second.contains(e.value())) { EventOfInterest exp_event =
         * createIntRemoveEvent(e.value()); notifyObservers(exp_event); } }
         *
         */

        @Override
        public Object master() {
            return IntExpOr.this;
        }

        @Override
        public String toString() {
            return "ExpOrObserver: ";
        }

    } // ~ ExpAddExpObserver
    private IntExp _exp1;
    private IntExp _exp2;

    private ExpressionObserver _observer1, _observer2;

    public IntExpOr(IntExp exp1, IntExp exp2) {
        super(exp1.constrainer(), "");// exp.name()+"+"+value);
        _exp1 = exp1;
        _exp2 = exp2;
        _exp1.attachObserver(_observer1 = new ExpOrExpObserver(_exp1));
        _exp2.attachObserver(_observer2 = new ExpOrExpObserver(_exp2));
    }

    @Override
    public boolean contains(int value) // better to be redefined
    {
        return _exp1.contains(value) || _exp2.contains(value);
    }

    public int max() {
        return Math.max(_exp1.max(), _exp2.max());
    }

    public int min() {
        return Math.min(_exp1.min(), _exp2.min());
    }

    @Override
    public void onMaskChange() {
        _observer1.publish(publisherMask(), _exp1);
        _observer2.publish(publisherMask(), _exp2);
    }

    @Override
    public void removeValue(int value) throws Failure {
        _exp1.removeValue(value);
        _exp2.removeValue(value);
    }

    public void setMax(int max) throws Failure {
        _exp1.setMax(max);
        _exp2.setMax(max);
    }

    public void setMin(int min) throws Failure {
        _exp1.setMin(min);
        _exp2.setMin(min);
    }

    @Override
    public void setValue(int value) throws Failure {
        _exp1.setValue(value);
        _exp2.setValue(value);
    }

    @Override
    public int value() throws Failure {
        return (_exp1.value() + _exp2.value()) / 2;
    }

} // eof IntExpAddExp
