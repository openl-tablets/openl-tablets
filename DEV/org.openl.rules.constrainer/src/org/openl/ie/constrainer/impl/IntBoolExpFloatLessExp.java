package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.Observer;
import org.openl.ie.constrainer.Subject;

/**
 * An implementation of the expression: <code>(FloatExp1 <= FloatExp2)</code>.
 */
public class IntBoolExpFloatLessExp extends IntBoolExpForSubject {
    class ObserverMinMax extends Observer {
        @Override
        public Object master() {
            return IntBoolExpFloatLessExp.this;
        }

        @Override
        public int subscriberMask() {
            return MIN | MAX;
        }

        @Override
        public void update(Subject subject, EventOfInterest interest) throws Failure {
            setDomainMinMax();
        }

    } // ~ObserverMinMax

    protected FloatExp _left, _right;
    protected double _offset;

    private Observer _observer;

    public IntBoolExpFloatLessExp(FloatExp left, FloatExp right) {
        this(left, right, 0, left.constrainer(), "");
    }

    public IntBoolExpFloatLessExp(FloatExp left, FloatExp right, double offset) {
        this(left, right, offset, left.constrainer(), "");
    }

    public IntBoolExpFloatLessExp(FloatExp left, FloatExp right, double offset, Constrainer c, String name) {
        super(c, name);
        _left = left;
        _right = right;
        _offset = offset;

        if (constrainer().showInternalNames()) {
            _name = "(" + left.name() + "||" + right.name() + ")";
        }
        setDomainMinMaxSafe();

        _observer = new ObserverMinMax();
        _left.attachObserver(_observer);
        _right.attachObserver(_observer);
    }

    @Override
    public boolean isLinear() {
        return (_left.isLinear() && _right.isLinear());
    }

    @Override
    public boolean isSubjectFalse() {
        return FloatCalc.gt(_left.min(), _right.max() + _offset);
    }

    @Override
    public boolean isSubjectTrue() {
        return FloatCalc.ge(_right.min() + _offset, _left.max());
    }

    @Override
    protected void setSubjectFalse() throws Failure {
        // left >= right + offset
        _left.setMin(_right.min() + _offset);
        _right.setMax(_left.max() - _offset);
    }

    @Override
    protected void setSubjectTrue() throws Failure {
        // left <= right + offset
        _left.setMax(_right.max() - _offset);
        _right.setMin(_left.min() + _offset);
    }

} // ~IntBoolExpFloatLessExp
