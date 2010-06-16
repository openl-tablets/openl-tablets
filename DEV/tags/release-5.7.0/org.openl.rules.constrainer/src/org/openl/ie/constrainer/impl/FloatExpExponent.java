package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatEvent;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.Subject;
import org.openl.ie.tools.Reusable;
import org.openl.ie.tools.ReusableFactory;

/**
 * An implementation of the expression: <code>exp(FloatExp)</code>.
 * <p>
 * Examples:
 * <p>
 * <code>FloatVar var = constrainer.addFloatVar(min,max,name);
 *  <p>
 *  FloatExp exp = var.exp();
 *  </code>
 */
public final class FloatExpExponent extends FloatExpImpl {
    static final class FloatEventExponent extends FloatEvent {

        static ReusableFactory _factory = new ReusableFactory() {
            @Override
            protected Reusable createNewElement() {
                return new FloatEventExponent();
            }

        };

        FloatEvent _event;

        int _type = 0;

        static FloatEventExponent getEvent(FloatEvent event, FloatExp exp) {
            FloatEventExponent ev = (FloatEventExponent) _factory.getElement();
            ev.init(event, exp);
            return ev;
        }

        void init(FloatEvent event, FloatExp exp_) {
            exp(exp_);
            _event = event;
            _type = 0;

            if (max() < oldmax()) {
                _type |= MAX;
            }

            if (min() > oldmin()) {
                _type |= MIN;
            }

            if (min() == max()) {
                _type |= VALUE;
            }
        }

        @Override
        public double max() {
            return Math.exp(_event.max());
        }

        @Override
        public double min() {
            return Math.exp(_event.min());
        }

        @Override
        public String name() {
            return "FloatEventExponent";
        }

        @Override
        public double oldmax() {
            return Math.exp(_event.oldmax());
        }

        @Override
        public double oldmin() {
            return Math.exp(_event.oldmin());
        }

        @Override
        public int type() {
            return _type;
        }

    }
    class FloatExpExponentObserver extends ExpressionObserver {

        FloatExpExponentObserver() {
            super(event_map);
        }

        @Override
        public Object master() {
            return FloatExpExponent.this;
        }

        @Override
        public String toString() {
            return "FloatExpExponentObserver: " + _exp;
        }

        @Override
        public void update(Subject exp, EventOfInterest event) throws Failure {
            FloatEvent e = (FloatEvent) event;

            FloatEventExponent ev = FloatEventExponent.getEvent(e, FloatExpExponent.this);

            notifyObservers(ev);
        }
    } // ~ FloatExpExponentObserver

    static final private int[] event_map = { MIN | MAX, MIN, MIN | MAX, MAX, VALUE, VALUE, REMOVE, REMOVE };

    private FloatExp _exp;

    private ExpressionObserver _observer;

    public FloatExpExponent(FloatExp exp) {
        super(exp.constrainer(), "");// "exp("+exp.name()+")");
        _exp = exp;
        _observer = new FloatExpExponentObserver();
        _exp.attachObserver(_observer);
    }

    public double max() {
        return Math.exp(_exp.max());
    }

    public double min() {
        return Math.exp(_exp.min());
    }

    @Override
    public void onMaskChange() {
        _observer.publish(publisherMask(), _exp);
    }

    public void setMax(double max) throws Failure {
        if (max < 0) {
            constrainer().fail("max < 0");
        }

        _exp.setMax(Math.log(max));
    }

    public void setMin(double min) throws Failure {
        if (min <= 0) {
            return;
        }

        _exp.setMin(Math.log(min));
    }

    public void setValue(double value) throws Failure {
        if (value <= 0) {
            constrainer().fail("value <= 0");
        }

        _exp.setValue(Math.log(value));
    }

    @Override
    public String toString() {
        return "exp(" + _exp + ")" + domainToString();
    }

} // ~FloatExpExponent
