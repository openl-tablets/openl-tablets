package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatEvent;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.Subject;
import org.openl.ie.tools.Reusable;
import org.openl.ie.tools.ReusableFactory;

/**
 * An implementation of the expression: <code>(1/FloatExp)</code>.
 * <p>
 * Examples:
 * <p>
 * <code>FloatVar var = constrainer.addFloatVar(min,max,name);
 *  <p>
 *  FloatExp exp = var.inverse();
 *  </code>
 */
public final class FloatExpInverse extends FloatExpImpl {
    static class FloatEventInverse extends FloatEvent {

        static ReusableFactory _factory = new ReusableFactory() {
            @Override
            protected Reusable createNewElement() {
                return new FloatEventInverse();
            }

        };

        FloatEvent _event;

        double _min, _max, _oldmin, _oldmax;
        int _type;
        static FloatEventInverse getEvent(FloatEvent event, FloatExpInverse exp) {
            FloatEventInverse ev = (FloatEventInverse) _factory.getElement();
            ev.init(event, exp);
            return ev;
        }

        public void init(FloatEvent e, FloatExpInverse exp) {
            exp(exp);

            _min = FloatCalc.inverseMin(e.min(), e.max());
            _max = FloatCalc.inverseMax(e.min(), e.max());
            _oldmin = FloatCalc.inverseMin(e.oldmin(), e.oldmax());
            _oldmax = FloatCalc.inverseMax(e.oldmin(), e.oldmax());
            FloatCalc.doAssert(_min >= _oldmin, "_min>=_oldmin");
            FloatCalc.doAssert(_max <= _oldmax, "_max>=_oldmax");

            _event = e;
            int type;
            _type = type = e.type();

            _type &= (MAX | MIN);
            if (_max < _oldmax) {
                _type |= MAX;
            }
            if (_min > _oldmin) {
                _type |= MIN;
            }
        }

        @Override
        public double max() {
            return _max;
        }

        @Override
        public double min() {
            return _min;
        }

        @Override
        public String name() {
            return "FloatEventInverse";
        }

        @Override
        public double oldmax() {
            return _oldmax;
        }

        @Override
        public double oldmin() {
            return _oldmin;
        }

        @Override
        public int type() {
            return _type;
        }

    } // ~ FloatEventInverse
    class FloatExpInverseObserver extends ExpressionObserver {

        FloatExpInverseObserver() {
            super(event_map);
        }

        @Override
        public Object master() {
            return FloatExpInverse.this;
        }

        @Override
        public String toString() {
            return "FloatExpInverseObserver: " + _exp;
        }

        @Override
        public void update(Subject exp, EventOfInterest event) throws Failure {
            FloatEvent e = (FloatEvent) event;

            FloatEventInverse ev = FloatEventInverse.getEvent(e, FloatExpInverse.this);

            notifyObservers(ev);
        }

    } // ~ FloatExpInverseObserver

    static final private int[] event_map = { MIN, MAX, MAX, MIN, VALUE, VALUE, REMOVE, REMOVE };

    private FloatExp _exp;

    private ExpressionObserver _observer;

    public FloatExpInverse(FloatExp exp) {
        super(exp.constrainer(), "");
        _exp = exp;
        _observer = new FloatExpInverseObserver();
        _exp.attachObserver(_observer);
    }

    public FloatExp inverse() {
        return _exp;
    }

    public double max() {
        return FloatCalc.inverseMax(_exp.min(), _exp.max());
    }

    public double min() {
        return FloatCalc.inverseMin(_exp.min(), _exp.max());
    }

    @Override
    public void onMaskChange() {
        _observer.publish(publisherMask(), _exp);
    }

    public void setMax(double max) throws Failure {
        // System.out.println("++-++ Set max: " + max + " in " + this);
        double min = min();
        double expMin = FloatCalc.inverseMin(min, max);
        double expMax = FloatCalc.inverseMax(min, max);
        _exp.setMin(expMin);
        _exp.setMax(expMax);
    }

    public void setMin(double min) throws Failure {
        // System.out.println("++++ Set min: " + min + " in " + this);
        double max = max();
        double expMin = FloatCalc.inverseMin(min, max);
        double expMax = FloatCalc.inverseMax(min, max);
        _exp.setMin(expMin);
        _exp.setMax(expMax);
    }

    public void setValue(double value) throws Failure {
        double expMin = FloatCalc.inverseMin(value);
        double expMax = FloatCalc.inverseMax(value);
        _exp.setMin(expMin);
        _exp.setMax(expMax);
    }

    @Override
    public String toString() {
        return "1/" + _exp + domainToString();
    }

} // ~FloatExpInverse
