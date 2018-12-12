package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatEvent;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.Subject;
import org.openl.ie.tools.Reusable;
import org.openl.ie.tools.ReusableFactory;


/**
 * An implementation of the expression: <code>(FloatExp + value)</code>.
 */
public final class FloatExpAddValue extends FloatExpImpl {
    /*
     * public double value() throws Failure { if (!bound())
     * constrainer().fail("Attempt to get value of the unbound expression
     * "+this); return _exp.value()+_value; }
     */
    static final class FloatEventAddValue extends FloatEvent {

        static ReusableFactory _factory = new ReusableFactory() {
            @Override
            protected Reusable createNewElement() {
                return new FloatEventAddValue();
            }

        };

        double _value;

        FloatEvent _event;
        static FloatEventAddValue getEvent(FloatEvent event, double value) {
            FloatEventAddValue ev = (FloatEventAddValue) _factory.getElement();
            ev.init(event, value);
            return ev;
        }

        public void init(FloatEvent e, double value) {
            _event = e;
            _value = value;
        }

        @Override
        public double max() {
            return _event.max() + _value;
        }

        @Override
        public double min() {
            return _event.min() + _value;
        }

        @Override
        public String name() {
            return "Event FloatAddValue";
        }

        @Override
        public double oldmax() {
            return _event.oldmax() + _value;
        }

        @Override
        public double oldmin() {
            return _event.oldmin() + _value;
        }

        @Override
        public int type() {
            return _event.type();
        }

    } // ~ FloatEventAddValue
    class FloatExpAddValueObserver extends ExpressionObserver {

        FloatExpAddValueObserver() {
        }

        @Override
        public Object master() {
            return FloatExpAddValue.this;
        }

        @Override
        public String toString() {
            return "FloatExpAddValueObserver: " + _exp + "+" + _value;
        }

        @Override
        public void update(Subject exp, EventOfInterest event) throws Failure {
            FloatEvent e = (FloatEvent) event;

            FloatEventAddValue ev = FloatEventAddValue.getEvent(e, _value);
            ev.exp(FloatExpAddValue.this);

            notifyObservers(ev);

        }

    } // ~ FloatExpAddValueObserver
    private FloatExp _exp;

    private double _value;

    private ExpressionObserver _observer;

    public FloatExpAddValue(FloatExp exp, double value) {
        super(exp.constrainer(), "");// exp.name()+"+"+value);
        _exp = exp;
        _value = value;
        _observer = new FloatExpAddValueObserver();
        _exp.attachObserver(_observer);
    }

    /**
     * Overloaded optimized implementation of the add(double)
     */
    @Override
    public FloatExp add(double value) {
        return _exp.add(_value + value);
    }

    @Override
    public boolean isLinear() {
        return _exp.isLinear();
    }

    public double max() {
        return _exp.max() + _value;
    }

    public double min() {
        return _exp.min() + _value;
    }

    @Override
    public void onMaskChange() {
        _observer.publish(publisherMask(), _exp);
    }

    // public void removeValue(double value) throws Failure
    // {
    // _exp.removeValue(value - _value);
    // }

    public void setMax(double max) throws Failure {
        _exp.setMax(max - _value);
    }

    public void setMin(double min) throws Failure {
        _exp.setMin(min - _value);
    }

    public void setValue(double value) throws Failure {
        _exp.setValue(value - _value);
    }

    @Override
    public double size() {
        return _exp.size();
    }

    @Override
    public String toString() {
        return "(" + _exp + " + " + _value + ")";
    }

} // ~FloatExpAddValue
