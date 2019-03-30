package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
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
/**
 * An implementation of the expression: <code>(IntBoolExp1 && IntBoolExp2)</code>.
 */
final class IntBoolExpAnd extends IntBoolExpForSubject {

    class ObserverBoolExpAnd extends Observer {
        IntBoolExp _exp2;

        public ObserverBoolExpAnd(IntBoolExp exp2) {
            _exp2 = exp2;
        }

        @Override
        public Object master() {
            return IntBoolExpAnd.this;
        }

        @Override
        public int subscriberMask() {
            return MIN | MAX;
        }

        @Override
        public void update(Subject subject, EventOfInterest interest) throws Failure {
            // exp1 is false
            if (interest.isMaxEvent()) {
                // (exp1 && exp2) is false
                setDomainMax(0);
            }
            // exp1 is true -> (exp1 && exp2) == exp2
            else {
                setDomainMin(_exp2.min());
                setDomainMax(_exp2.max());
                _exp2.setMin(_min);
                _exp2.setMax(_max);
            }
        }

    }

    private IntBoolExp _exp1, _exp2;

    public IntBoolExpAnd(IntBoolExp exp1, IntBoolExp exp2) {
        super(exp1.constrainer());
        _exp1 = exp1;
        _exp2 = exp2;

        if (constrainer().showInternalNames()) {
            _name = "(" + exp1.name() + "&&" + exp2.name() + ")";
        }
        setDomainMinMaxSafe();

        _exp1.attachObserver(new ObserverBoolExpAnd(_exp2));
        _exp2.attachObserver(new ObserverBoolExpAnd(_exp1));
    }

    @Override
    protected boolean isSubjectFalse() {
        return _exp1.isFalse() || _exp2.isFalse();
    }

    @Override
    protected boolean isSubjectTrue() {
        return _exp1.isTrue() && _exp2.isTrue();
    }

    @Override
    protected void setSubjectFalse() throws Failure {
        if (_exp1.isTrue()) {
            _exp2.setFalse();
        }
        if (_exp2.isTrue()) {
            _exp1.setFalse();
        }
    }

    @Override
    protected void setSubjectTrue() throws Failure {
        _exp1.setTrue();
        _exp2.setTrue();
    }

} // ~IntBoolExpAnd
