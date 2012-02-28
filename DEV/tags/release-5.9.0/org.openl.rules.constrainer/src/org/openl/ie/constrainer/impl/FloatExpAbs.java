package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatEvent;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.FloatVar;
import org.openl.ie.constrainer.Observer;
import org.openl.ie.constrainer.Subject;

/**
 * An implementation of the expression: <code>abs(FloatExp)</code>.
 * <p>
 * Examples:
 * <p>
 * <code>FloatVar var = constrainer.addFloatVar(min,max,name);
 *  <p>
 *  FloatExp exp = var.abs();
 *  </code>
 */
public final class FloatExpAbs extends FloatExpImpl {
    class FloatExpAbsObserver extends ExpressionObserver {

        FloatExpAbsObserver() {
            // super(event_map);
        }

        @Override
        public Object master() {
            return FloatExpAbs.this;
        }

        @Override
        public int subscriberMask() {
            return MIN | MAX | VALUE;
        }

        @Override
        public String toString() {
            return "FloatExpAbsObserver: " + _exp;
        }

        @Override
        public void update(Subject exp, EventOfInterest event) throws Failure {
            FloatEvent e = (FloatEvent) event;

            // FloatEventAbs ev = FloatEventAbs.getEvent(e,FloatExpAbs.this);
            // notifyObservers(ev);

            double min = e.min();
            double max = e.max();
            _abs.setMin(abs_min(min, max));
            _abs.setMax(abs_max(min, max));
        }

    } // ~ FloatExpAbsObserver
    private FloatExp _exp;

    private Observer _observer;

    // static final private int[] event_map = { MIN | MAX, MIN,
    // MIN | MAX, MAX,
    // VALUE, VALUE,
    // REMOVE, REMOVE
    // };

    private FloatVar _abs;

    /**
     * @return max(abs(min),abs(max))
     */
    static double abs_max(double min, double max) {
        // min >= 0 && max >= 0
        if (min >= 0) {
            return max;
        }

        // min < 0 && max <= 0
        if (max <= 0) {
            return -min;
        }

        // min < 0 && max > 0
        return Math.max(-min, max);

    }

    /**
     * @return min(abs(min),abs(max))
     */
    static double abs_min(double min, double max) {
        // min >= 0 && max >= 0
        if (min >= 0) {
            return min;
        }

        // min < 0 && max >= 0
        if (max >= 0) {
            return 0;
        }

        // min < 0 && max < 0
        return -max;
    }

    public FloatExpAbs(FloatExp exp) {
        super(exp.constrainer());
        _exp = exp;
        _observer = new FloatExpAbsObserver();
        _exp.attachObserver(_observer);

        if (constrainer().showInternalNames()) {
            _name = "|" + exp.name() + "|";
        }

        // int trace = IntVarImplTrace.TRACE_ALL;
        int trace = 0;
        double min = _exp.min();
        double max = _exp.max();
        _abs = constrainer().addFloatVarTraceInternal(abs_min(min, max), abs_max(min, max), _name, trace);
    }

    @Override
    public void attachObserver(Observer observer) {
        super.attachObserver(observer);
        _abs.attachObserver(observer);
    }

    @Override
    public void detachObserver(Observer observer) {
        super.detachObserver(observer);
        _abs.detachObserver(observer);
    }

    public double max() {
        // return abs_max(_exp.min(), _exp.max());
        return _abs.max();
    }

    public double min() {
        // return abs_min( _exp.min(), _exp.max());
        return _abs.min();
    }

    @Override
    public void onMaskChange() {
        // _observer.publish(publisherMask(),_exp);
    }

    @Override
    public void reattachObserver(Observer observer) {
        super.reattachObserver(observer);
        _abs.reattachObserver(observer);
    }

    public void setMax(double max) throws Failure {
        _abs.setMax(max);

        _exp.setMax(max);
        _exp.setMin(-max);
    }

    public void setMin(double min) throws Failure {
        if (min <= 0) {
            return;
        }

        _abs.setMin(min);

        double removeMax = min;

        _exp.removeRange(-removeMax, removeMax);
    }

    public void setValue(double value) throws Failure {
        _abs.setValue(value);

        setMax(value);
        setMin(value);
    }

    @Override
    public String toString() {
        return "|" + _exp + "|" + domainToString();
    }

    // static final class FloatEventAbs extends FloatEvent
    // {
    //
    // static ReusableFactory _factory = new ReusableFactory()
    // {
    // protected Reusable createNewElement()
    // {
    // return new FloatEventAbs();
    // }
    //
    // };
    //
    // static FloatEventAbs getEvent(FloatEvent event, FloatExp exp)
    // {
    // FloatEventAbs ev = (FloatEventAbs) _factory.getElement();
    // ev.init(event,exp);
    // return ev;
    // }
    //
    // FloatEvent _event;
    //
    // int _type = 0;
    //
    // void init(FloatEvent event, FloatExp exp_)
    // {
    // exp(exp_);
    // _event = event;
    // _type = 0;
    //
    // if (max() < oldmax())
    // {
    // _type |= MAX;
    // }
    //
    // if (min() > oldmin())
    // {
    // _type |= MIN;
    // }
    //
    // if (min() == max())
    // _type |= VALUE;
    // }
    //
    //
    // public int type()
    // {
    // return _type;
    // }
    //
    //
    // public double oldmax()
    // {
    // return FloatExpAbs.abs_max(_event.oldmin(), _event.oldmax());
    // }
    //
    // public double oldmin()
    // {
    // return FloatExpAbs.abs_min(_event.oldmin(), _event.oldmax());
    // }
    //
    // public double max()
    // {
    // return FloatExpAbs.abs_max(_event.min(), _event.max());
    // }
    //
    // public double min()
    // {
    // return FloatExpAbs.abs_min(_event.min(), _event.max());
    // }
    //
    //
    // public String name()
    // {
    // return "FloatEventAbs";
    // }
    //
    // }

} // ~FloatExpAbs
