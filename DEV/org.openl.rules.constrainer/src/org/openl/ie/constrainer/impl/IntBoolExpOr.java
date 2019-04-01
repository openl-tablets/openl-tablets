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
 * An implementation of the expression: <code>(IntBoolExp1 || IntBoolExp2)</code>.
 */
final class IntBoolExpOr extends IntBoolExpForSubject {
    class ObserverBoolExpOr extends Observer {
        IntBoolExp _exp2;

        public ObserverBoolExpOr(IntBoolExp exp2) {
            _exp2 = exp2;
        }

        @Override
        public Object master() {
            return IntBoolExpOr.this;
        }

        @Override
        public int subscriberMask() {
            return MIN | MAX;
        }

        @Override
        public void update(Subject subject, EventOfInterest interest) throws Failure {
            // exp1 is true
            if (interest.isMinEvent()) {
                // (exp1 || exp2) is true
                setDomainMin(1);
            }
            // exp1 is false -> (exp1 || exp2) == exp2
            else {
                setDomainMin(_exp2.min());
                setDomainMax(_exp2.max());
                _exp2.setMin(_min);
                _exp2.setMax(_max);
            }
        }

    }

    private IntBoolExp _exp1, _exp2;

    public IntBoolExpOr(IntBoolExp exp1, IntBoolExp exp2) {
        super(exp1.constrainer());
        _exp1 = exp1;
        _exp2 = exp2;

        if (constrainer().showInternalNames()) {
            _name = "(" + exp1.name() + "||" + exp2.name() + ")";
        }
        setDomainMinMaxSafe();

        _exp1.attachObserver(new ObserverBoolExpOr(_exp2));
        _exp2.attachObserver(new ObserverBoolExpOr(_exp1));
    }

    @Override
    protected boolean isSubjectFalse() {
        return _exp1.isFalse() && _exp2.isFalse();
    }

    @Override
    protected boolean isSubjectTrue() {
        return _exp1.isTrue() || _exp2.isTrue();
    }

    @Override
    protected void setSubjectFalse() throws Failure {
        _exp1.setFalse();
        _exp2.setFalse();
    }

    @Override
    protected void setSubjectTrue() throws Failure {
        if (_exp1.isFalse()) {
            _exp2.setTrue();
        }
        if (_exp2.isFalse()) {
            _exp1.setTrue();
        }
    }

} // ~IntBoolExpOr
