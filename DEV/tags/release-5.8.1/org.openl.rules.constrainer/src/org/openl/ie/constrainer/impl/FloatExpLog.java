package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatEvent;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.Subject;
import org.openl.ie.tools.Reusable;
import org.openl.ie.tools.ReusableFactory;

/**
 * An implementation of the expression: <code>log(FloatExp)</code>.
 * <p>
 * Examples:
 * <p>
 * <code>FloatVar var = constrainer.addFloatVar(min,max,name);
 *  <p>
 *  FloatExp exp = var.log();
 *  </code>
 */
public final class FloatExpLog extends FloatExpImpl {
    static final class FloatEventLog extends FloatEvent {

        static ReusableFactory _factory = new ReusableFactory() {
            @Override
            protected Reusable createNewElement() {
                return new FloatEventLog();
            }

        };

        FloatEvent _event;

        int _type = 0;

        static FloatEventLog getEvent(FloatEvent event, FloatExp exp) {
            FloatEventLog ev = (FloatEventLog) _factory.getElement();
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
            return Math.log(_event.max());
        }

        @Override
        public double min() {
            return Math.log(_event.min());
        }

        @Override
        public String name() {
            return "FloatEventLog";
        }

        @Override
        public double oldmax() {
            return Math.log(_event.oldmax());
        }

        @Override
        public double oldmin() {
            return Math.log(_event.oldmin());
        }

        @Override
        public int type() {
            return _type;
        }

    }
    class FloatExpLogObserver extends ExpressionObserver {

        FloatExpLogObserver() {
            super(event_map);
        }

        @Override
        public Object master() {
            return FloatExpLog.this;
        }

        @Override
        public String toString() {
            return "FloatExpLogObserver: " + _exp;
        }

        @Override
        public void update(Subject exp, EventOfInterest event) throws Failure {
            FloatEvent e = (FloatEvent) event;

            FloatEventLog ev = FloatEventLog.getEvent(e, FloatExpLog.this);

            notifyObservers(ev);
        }
    } // ~ FloatExpLogObserver

    static final private int[] event_map = { MIN | MAX, MIN, MIN | MAX, MAX, VALUE, VALUE, REMOVE, REMOVE };

    private FloatExp _exp;

    private ExpressionObserver _observer;

    public FloatExpLog(FloatExp exp) {
        super(exp.constrainer(), "");// "log("+exp.name()+")");
        _exp = exp;
        _observer = new FloatExpLogObserver();
        _exp.attachObserver(_observer);
    }

    public double max() {
        return Math.log(_exp.max());
    }

    public double min() {
        return Math.log(_exp.min());
    }

    @Override
    public void onMaskChange() {
        _observer.publish(publisherMask(), _exp);
    }

    public void setMax(double max) throws Failure {
        _exp.setMax(Math.exp(max));
    }

    public void setMin(double min) throws Failure {
        _exp.setMin(Math.exp(min));
    }

    public void setValue(double value) throws Failure {
        _exp.setValue(Math.exp(value));
    }

    @Override
    public String toString() {
        return "log(" + _exp + ")" + domainToString();
    }

} // ~FloatExpLog
