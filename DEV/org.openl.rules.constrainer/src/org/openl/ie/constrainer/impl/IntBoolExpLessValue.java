package org.openl.ie.constrainer.impl;

import java.util.Map;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.NonLinearExpression;
import org.openl.ie.constrainer.Observer;
import org.openl.ie.constrainer.Subject;


/**
 * An implementation of the expression: <code>(IntExp < value)</code>.
 */
public class IntBoolExpLessValue extends IntBoolExpForSubject {
    class ObserverMinMax extends Observer {
        @Override
        public Object master() {
            return IntBoolExpLessValue.this;
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
    protected IntExp _left;
    protected int _right;

    private Observer _observer;

    public IntBoolExpLessValue(IntExp left, int right) {
        this(left, right, left.constrainer(), "");
    }

    public IntBoolExpLessValue(IntExp left, int right, Constrainer c, String name) {
        super(c, name);

        _left = left;
        _right = right;

        if (constrainer().showInternalNames()) {
            _name = "(" + left.name() + "<" + right + ")";
        }
        setDomainMinMaxSafe();

        _observer = new ObserverMinMax();
        _left.attachObserver(_observer);
    }

    @Override
    public double calcCoeffs(Map map, double factor) throws NonLinearExpression {
        return (_left.neg().add(_right)).calcCoeffs(map, factor);
    }

    @Override
    public boolean isLinear() {
        return (_left.isLinear());
    }

    @Override
    protected boolean isSubjectFalse() {
        return _left.min() >= _right;
    }

    @Override
    protected boolean isSubjectTrue() {
        return _left.max() < _right;
    }

    @Override
    protected void setSubjectFalse() throws Failure {
        // left >= right
        _left.setMin(_right);
    }

    @Override
    protected void setSubjectTrue() throws Failure {
        // left < right
        _left.setMax(_right - 1);
    }

} // ~IntBoolExpLessValue
