package org.openl.ie.constrainer.impl;

import java.util.Map;

import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.NonLinearExpression;
import org.openl.ie.constrainer.Observer;
import org.openl.ie.constrainer.Subject;


/**
 * An implementation of the expression: <code>(IntExp == IntExp + offset)</code>.
 */
public final class IntBoolExpEqExp extends IntBoolExpForSubject {
    final class ObserverMinMax extends Observer {
        @Override
        public Object master() {
            return IntBoolExpEqExp.this;
        }

        @Override
        public int subscriberMask() {
            return ALL;
        }

        @Override
        public void update(Subject exp, EventOfInterest interest) throws Failure {
            /*
             * IntEvent event = (IntEvent)interest; if(isTrue() &&
             * event.isRemoveEvent()) { int max = event.numberOfRemoves();
             * for(int i=0; i < max; ++i) { // if (exp == _exp1)
             * _exp2.removeValue(event.removed(i)-_offset); // else if (exp ==
             * _exp2) _exp1.removeValue(event.removed(i)+_offset); } }
             */
            setDomainMinMax();
        }

    } // ~ObserverMinMax
    private IntExp _exp1, _exp2;
    private int _offset;

    private Observer _observer;

    public IntBoolExpEqExp(IntExp exp1, IntExp exp2) {
        this(exp1, exp2, 0);
    }

    public IntBoolExpEqExp(IntExp exp1, IntExp exp2, int offset) {
        super(exp1.constrainer());

        _exp1 = exp1;
        _exp2 = exp2;
        _offset = offset;

        if (constrainer().showInternalNames()) {
            if (offset == 0) {
                _name = "(" + exp1.name() + "==" + exp2.name() + ")";
            } else if (offset > 0) {
                _name = "(" + exp1.name() + "==" + exp2.name() + "+" + offset + ")";
            } else {
                _name = "(" + exp1.name() + "==" + exp2.name() + offset + ")";
            }
        }

        setDomainMinMaxSafe();

        _observer = new ObserverMinMax();
        _exp1.attachObserver(_observer);
        _exp2.attachObserver(_observer);
    }

    @Override
    public double calcCoeffs(Map map, double factor) throws NonLinearExpression {
        return (_exp2.sub(_exp1).sub(_offset)).calcCoeffs(map, factor);
    }

    // public Constraint asConstraint()
    // {
    // return _exp1.equals(_exp2,_offset);
    // }
    @Override
    public boolean isLinear() {
        return (_exp1.isLinear() && _exp2.isLinear());
    }

    @Override
    protected boolean isSubjectFalse() {
        // exp1 > exp2 || exp1 < exp2
        return _exp1.min() > _exp2.max() + _offset || _exp1.max() < _exp2.min() + _offset;
    }

    @Override
    protected boolean isSubjectTrue() {
        // both are bound and equals
        return _exp1.min() == _exp2.max() + _offset && _exp1.max() == _exp2.min() + _offset;
    }

    @Override
    protected void setSubjectFalse() throws Failure {
        // exp1 != exp2
        if (_exp2.bound()) {
            _exp1.removeValue(_exp2.value() + _offset);
        }
        if (_exp1.bound()) {
            _exp2.removeValue(_exp1.value() - _offset);
        }
    }

    @Override
    protected void setSubjectTrue() throws Failure {
        // exp1 == exp2
        _exp1.setMax(_exp2.max() + _offset);
        _exp2.setMax(_exp1.max() - _offset);
        _exp1.setMin(_exp2.min() + _offset);
        _exp2.setMin(_exp1.min() - _offset);
    }
} // ~IntBoolExpEqExp
