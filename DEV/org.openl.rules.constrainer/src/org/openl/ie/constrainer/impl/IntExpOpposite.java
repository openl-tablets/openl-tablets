package org.openl.ie.constrainer.impl;

import java.util.Map;

import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.NonLinearExpression;
import org.openl.ie.constrainer.Subject;
import org.openl.ie.tools.Reusable;
import org.openl.ie.tools.ReusableFactory;


//
//: IntExpOpposite.java
//
/**
 * An implementation of the expression: <code>(-IntExp)</code>.
 */
public final class IntExpOpposite extends IntExpImpl {
    class ExpOppositeObserver extends ExpressionObserver {
        IntExp _exp_this;

        ExpOppositeObserver(IntExp exp_this, int[] event_map) {
            super(event_map);
            _exp_this = exp_this;
        }

        @Override
        public Object master() {
            return IntExpOpposite.this;
        }

        @Override
        public String toString() {
            return "ExpOppositeObserver: " + _exp;
        }

        @Override
        public void update(Subject exp, EventOfInterest event) throws Failure {
            IntEvent e = (IntEvent) event;

            IntEventOpposite ev = IntEventOpposite.getEvent(e);

            ev.exp(_exp_this);

            notifyObservers(ev);

        }

    } // ~ ExpOppositeObserver
    static class IntEventOpposite extends IntEvent {

        static ReusableFactory _factory = new ReusableFactory() {
            @Override
            protected Reusable createNewElement() {
                return new IntEventOpposite();
            }

        };

        IntEvent _event;

        int _type;
        static IntEventOpposite getEvent(IntEvent event) {
            IntEventOpposite ev = (IntEventOpposite) _factory.getElement();
            ev.init(event);
            return ev;
        }

        public void init(IntEvent e) {
            _event = e;
            int type;
            _type = type = e.type();

            _type |= MAX | MIN;
            if ((type & MIN) == 0) {
                _type &= ~MAX;
            }
            if ((type & MAX) == 0) {
                _type &= ~MIN;
            }
        }

        @Override
        public int max() {
            return -_event.min();
        }

        @Override
        public int min() {
            return -_event.max();
        }

        @Override
        public String name() {
            return "Event Opposite";
        }

        @Override
        public int numberOfRemoves() {
            return _event.numberOfRemoves();
        }

        @Override
        public int oldmax() {
            return -_event.oldmin();
        }

        @Override
        public int oldmin() {
            return -_event.oldmax();
        }

        @Override
        public int removed(int i) {
            return -_event.removed(i);
        }

        @Override
        public int type() {
            return _type;
        }

    }

    static final private int[] event_map = { MIN, MAX, MAX, MIN, VALUE, VALUE, REMOVE, REMOVE };

    private IntExp _exp;

    private ExpressionObserver _observer;

    // public IntExp negI()
    // {
    // return _exp;
    // }

    public IntExpOpposite(IntExp exp) {
        super(exp.constrainer());

        if (constrainer().showInternalNames()) {
            _name = "(-" + exp.name() + ")";
        }

        _exp = exp;
        _observer = new ExpOppositeObserver(this, event_map);
        _exp.attachObserver(_observer);
    }

    @Override
    public boolean bound() {
        return _exp.bound();
    }

    @Override
    public double calcCoeffs(Map map, double factor) throws NonLinearExpression {
        return _exp.calcCoeffs(map, -1 * factor);
    }

    @Override
    public boolean contains(int value) {
        return _exp.contains(-value);
    }

    @Override
    public boolean isLinear() {
        return _exp.isLinear();
    }

    public int max() {
        return -_exp.min();
    }

    public int min() {
        return -_exp.max();
    }

    /**
     * added by ET 02.12.03 for optimization
     */
    @Override
    public IntExp neg() {
        return _exp;
    }

    @Override
    public void onMaskChange() {
        _observer.publish(publisherMask(), _exp);
    }

    @Override
    public void removeValue(int value) throws Failure {
        _exp.removeValue(-value);
    }

    public void setMax(int max) throws Failure {
        // System.out.println("++-++ Set max: " + max + " in " + this);

        _exp.setMin(-max);
    }

    public void setMin(int min) throws Failure {
        // System.out.println("++++ Set min: " + min + " in " + this);

        _exp.setMax(-min);
    }

    @Override
    public void setValue(int value) throws Failure {
        _exp.setValue(-value);
    }

    @Override
    public int size() {
        return _exp.size();
    }

    @Override
    public int value() throws Failure {
        if (!_exp.bound()) {
            constrainer().fail("Attempt to get value of the unbound expression " + this);
        }
        return -_exp.value();
    }

} // eof IntExpOpposite
