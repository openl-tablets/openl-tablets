package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatEvent;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.Subject;
import org.openl.ie.tools.Reusable;
import org.openl.ie.tools.ReusableFactory;

/**
 * An implementation of the expression: <code>sqr(FloatExp)</code>.
 * <p>
 * Examples:
 * <p>
 * <code>FloatVar var = constrainer.addFloatVar(min,max,name);
 *  <p>
 *  FloatExp exp = var.sqr();
 *  </code>
 */
public final class FloatExpSqr extends FloatExpImpl {
    static final class FloatEventSqr extends FloatEvent {

        static ReusableFactory _factory = new ReusableFactory() {
            @Override
            protected Reusable createNewElement() {
                return new FloatEventSqr();
            }

        };

        FloatEvent _event;

        int _type = 0;

        static FloatEventSqr getEvent(FloatEvent event, FloatExp exp) {
            FloatEventSqr ev = (FloatEventSqr) _factory.getElement();
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
            return FloatCalc.sqrMax(_event.min(), _event.max());
        }

        @Override
        public double min() {
            return FloatCalc.sqrMin(_event.min(), _event.max());
        }

        @Override
        public String name() {
            return "FloatEventSqr";
        }

        @Override
        public double oldmax() {
            return FloatCalc.sqrMax(_event.oldmin(), _event.oldmax());
        }

        @Override
        public double oldmin() {
            return FloatCalc.sqrMin(_event.oldmin(), _event.oldmax());
        }

        @Override
        public int type() {
            return _type;
        }

    }
    class FloatExpSqrObserver extends ExpressionObserver {

        FloatExpSqrObserver() {
            super(event_map);
        }

        @Override
        public Object master() {
            return FloatExpSqr.this;
        }

        @Override
        public String toString() {
            return "FloatExpSqrObserver: " + _exp;
        }

        @Override
        public void update(Subject exp, EventOfInterest event) throws Failure {
            FloatEvent e = (FloatEvent) event;

            FloatEventSqr ev = FloatEventSqr.getEvent(e, FloatExpSqr.this);

            notifyObservers(ev);
        }
    } // ~ FloatExpSqrObserver

    static final private int[] event_map = { MIN | MAX, MIN, MIN | MAX, MAX, VALUE, VALUE, REMOVE, REMOVE };

    private FloatExp _exp;

    private ExpressionObserver _observer;

    public FloatExpSqr(FloatExp exp) {
        super(exp.constrainer(), "");// "sqr("+exp.name()+")");
        _exp = exp;
        _observer = new FloatExpSqrObserver();
        _exp.attachObserver(_observer);
    }

    public double max() {
        return FloatCalc.sqrMax(_exp.min(), _exp.max());
    }

    public double min() {
        return FloatCalc.sqrMin(_exp.min(), _exp.max());
    }

    @Override
    public void onMaskChange() {
        _observer.publish(publisherMask(), _exp);
    }

    public void setMax(double max) throws Failure {
        if (max < 0) {
            constrainer().fail("max < 0");
        }

        double expMax = Math.sqrt(max);
        _exp.setMax(expMax);
        _exp.setMin(-expMax);
    }

    public void setMin(double min) throws Failure {
        if (min <= 0) {
            return;
        // exclude range [-Math.sqrt(min)..Math.sqrt(min)] ???
        }
    }

    public void setValue(double value) throws Failure {
        if (value < 0) {
            constrainer().fail("value < 0");
        }

        _exp.setValue(Math.sqrt(value));
    }

    @Override
    public String toString() {
        return "sqr(" + _exp + ")" + domainToString();
    }

} // ~FloatExpSqr
