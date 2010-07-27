package org.openl.ie.constrainer.impl;

import java.util.Map;

import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatEvent;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.NonLinearExpression;
import org.openl.ie.constrainer.Subject;
import org.openl.ie.tools.Reusable;
import org.openl.ie.tools.ReusableFactory;


/**
 * An implementation of the expression: <code>(-FloatExp)</code>.
 * <p>
 * Examples:
 * <p>
 * <code>FloatVar var = constrainer.addFloatVar(min,max,name);
 *  <p>
 *  FloatExp exp = var.negF();
 *  </code>
 */
public final class FloatExpOpposite extends FloatExpImpl {
    static class FloatEventOpposite extends FloatEvent {

        static ReusableFactory _factory = new ReusableFactory() {
            @Override
            protected Reusable createNewElement() {
                return new FloatEventOpposite();
            }

        };

        FloatEvent _event;

        int _type;
        static FloatEventOpposite getEvent(FloatEvent event) {
            FloatEventOpposite ev = (FloatEventOpposite) _factory.getElement();
            ev.init(event);
            return ev;
        }

        public void init(FloatEvent e) {
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
        public double max() {
            return -_event.min();
        }

        @Override
        public double min() {
            return -_event.max();
        }

        @Override
        public String name() {
            return "FloatEventOpposite";
        }

        @Override
        public double oldmax() {
            return -_event.oldmin();
        }

        @Override
        public double oldmin() {
            return -_event.oldmax();
        }

        @Override
        public int type() {
            return _type;
        }

    } // ~ FloatEventOpposite
    class FloatExpOppositeObserver extends ExpressionObserver {

        FloatExpOppositeObserver() {
            super(event_map);
        }

        @Override
        public Object master() {
            return FloatExpOpposite.this;
        }

        @Override
        public String toString() {
            return "FloatExpOppositeObserver: " + _exp;
        }

        @Override
        public void update(Subject exp, EventOfInterest event) throws Failure {
            FloatEvent e = (FloatEvent) event;

            FloatEventOpposite ev = FloatEventOpposite.getEvent(e);
            ev.exp(FloatExpOpposite.this);

            notifyObservers(ev);
        }

    } // ~ FloatExpOppositeObserver

    static final private int[] event_map = { MIN, MAX, MAX, MIN, VALUE, VALUE, REMOVE, REMOVE };

    private FloatExp _exp;

    private ExpressionObserver _observer;

    public FloatExpOpposite(FloatExp exp) {
        super(exp.constrainer(), "");
        _exp = exp;
        _observer = new FloatExpOppositeObserver();
        _exp.attachObserver(_observer);
    }

    @Override
    public double calcCoeffs(Map map, double factor) throws NonLinearExpression {
        return _exp.calcCoeffs(map, -1 * factor);
    }

    @Override
    public boolean isLinear() {
        return _exp.isLinear();
    }

    public double max() {
        return -_exp.min();
    }

    public double min() {
        return -_exp.max();
    }

    public FloatExp negF() {
        return _exp;
    }

    @Override
    public void onMaskChange() {
        _observer.publish(publisherMask(), _exp);
    }

    public void setMax(double max) throws Failure {
        // System.out.println("++-++ Set max: " + max + " in " + this);

        _exp.setMin(-max);
    }

    public void setMin(double min) throws Failure {

        // System.out.println("++++ Set min: " + min + " in " + this);

        _exp.setMax(-min);
    }

    public void setValue(double value) throws Failure {
        _exp.setValue(-value);
    }

    @Override
    public String toString() {
        return "-" + _exp + domainToString();
    }

} // ~FloatExpOpposite
