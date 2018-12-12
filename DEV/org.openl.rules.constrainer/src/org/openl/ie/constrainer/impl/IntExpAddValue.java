package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.Subject;
import org.openl.ie.tools.Reusable;
import org.openl.ie.tools.ReusableFactory;


//
//: IntExpAddValue.java
//
/**
 * An implementation of the expression: <code>(IntExp + value)</code>.
 */
public final class IntExpAddValue extends IntExpImpl {
    class ExpAddValueObserver extends ExpressionObserver {
        IntExp _exp_this;

        ExpAddValueObserver(IntExp exp_this) {
            _exp_this = exp_this;
        }

        @Override
        public Object master() {
            return IntExpAddValue.this;
        }

        @Override
        public String toString() {
            return "ExpAddValueObserver: " + _exp + "+" + _value;
        }

        @Override
        public void update(Subject exp, EventOfInterest event) throws Failure {
            IntEvent e = (IntEvent) event;

            IntEventAddValue ev = IntEventAddValue.getEvent(e, _value);
            ev.exp(_exp_this);

            notifyObservers(ev);

        }

    } // ~ ExpAddValueObserver
    static final class IntEventAddValue extends IntEvent {

        static ReusableFactory _factory = new ReusableFactory() {
            @Override
            protected Reusable createNewElement() {
                return new IntEventAddValue();
            }

        };

        int _value;

        IntEvent _event;
        static IntEventAddValue getEvent(IntEvent event, int value) {
            IntEventAddValue ev = (IntEventAddValue) _factory.getElement();
            ev.init(event, value);
            return ev;
        }

        public void init(IntEvent e, int value) {
            _event = e;
            _value = value;
        }

        @Override
        public int max() {
            return _event.max() + _value;
        }

        @Override
        public int min() {
            return _event.min() + _value;
        }

        @Override
        public String name() {
            return "Event AddValue";
        }

        @Override
        public int numberOfRemoves() {
            return _event.numberOfRemoves();
        }

        @Override
        public int oldmax() {
            return _event.oldmax() + _value;
        }

        @Override
        public int oldmin() {
            return _event.oldmin() + _value;
        }

        @Override
        public int removed(int i) {
            return _event.removed(i) + _value;
        }

        @Override
        public int type() {
            return _event.type();
        }

    } // ~IntEventAddValue
    private IntExp _exp;

    private int _value;

    private ExpressionObserver _observer;

    public IntExpAddValue(IntExp exp, int value) {
        super(exp.constrainer());

        if (constrainer().showInternalNames()) {
            _name = "(" + exp.name() + "+" + value + ")";
        }

        _exp = exp;
        _value = value;
        _observer = new ExpAddValueObserver(this);
        _exp.attachObserver(_observer);
    }

    @Override
    public IntExp add(int value) {
        return _exp.add(_value + value);
    }

    @Override
    public boolean bound() {
        return _exp.bound();
    }

    @Override
    public boolean contains(int value) {
        return _exp.contains(value - _value);
    }

    @Override
    public boolean isLinear() {
        return _exp.isLinear();
    }

    public int max() {
        return _exp.max() + _value;
    }

    public int min() {
        return _exp.min() + _value;
    }

    @Override
    public void onMaskChange() {
        _observer.publish(publisherMask(), _exp);
    }

    @Override
    public void removeValue(int value) throws Failure {
        _exp.removeValue(value - _value);
    }

    public void setMax(int max) throws Failure {
        _exp.setMax(max - _value);
    }

    public void setMin(int min) throws Failure {

        // System.out.println("++++ Set min: " + min + " in " + this);

        _exp.setMin(min - _value);
    }

    @Override
    public void setValue(int value) throws Failure {
        _exp.setValue(value - _value);
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
        return _exp.value() + _value;
    }

} // ~IntExpAddValue
