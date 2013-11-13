package org.openl.ie.constrainer.impl;

import java.util.Map;

import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatEvent;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.NonLinearExpression;
import org.openl.ie.constrainer.Subject;
import org.openl.ie.tools.Reusable;
import org.openl.ie.tools.ReusableFactory;


/**
 * An implementation of the expression: <code>FloatExp(IntExp)</code>.
 */
public final class FloatExpIntExp extends FloatExpImpl {
    static final class FloatEventIntEvent extends FloatEvent {

        static ReusableFactory _factory = new ReusableFactory() {
            @Override
            protected Reusable createNewElement() {
                return new FloatEventIntEvent();
            }

        };

        IntEvent _event;

        static FloatEventIntEvent getEvent(IntEvent event, FloatExp exp) {
            FloatEventIntEvent ev = (FloatEventIntEvent) _factory.getElement();
            ev.init(event, exp);
            return ev;
        }

        public void init(IntEvent e, FloatExp exp_) {
            exp(exp_);
            _event = e;
        }

        @Override
        public double max() {
            return _event.max();
        }

        @Override
        public double min() {
            return _event.min();
        }

        @Override
        public String name() {
            return "Event FloatEventIntEvent";
        }

        @Override
        public double oldmax() {
            return _event.oldmax();
        }

        @Override
        public double oldmin() {
            return _event.oldmin();
        }

        @Override
        public int type() {
            return _event.type();
        }

    } // ~ FloatEventIntEvent
    /**
     * Class <code>FloatExpIntExpObserver</code> wrap event from integer
     * expression into float event and notify observers of float expression.
     */
    class FloatExpIntExpObserver extends ExpressionObserver {
        FloatExpIntExpObserver() {
            super(event_map);
        }

        @Override
        public Object master() {
            return FloatExpIntExp.this;
        }

        @Override
        public String toString() {
            return "FloatExpIntExpObserver: ";
        }

        @Override
        public void update(Subject exp, EventOfInterest event) throws Failure {
            IntEvent e = (IntEvent) event;

            FloatEventIntEvent ev = FloatEventIntEvent.getEvent(e, FloatExpIntExp.this);

            notifyObservers(ev);
        }

    } // ~ FloatExpIntExpObserver

    static final private int[] event_map = { MIN, MIN, MAX, MAX, VALUE, VALUE, REMOVE, REMOVE, };

    /**
     * The methods bound() and value() can be implemented following either
     * semantic of the float expression (as in ILOG's Solver) OR the underlyng
     * integer expression.
     */
    private static final boolean AS_FLOAT_EXP = true;

    private IntExp _exp;

    private ExpressionObserver _observer;

    public FloatExpIntExp(IntExp exp) {
        super(exp.constrainer(), exp.name());
        _exp = exp;

        _observer = new FloatExpIntExpObserver();
        _exp.attachObserver(_observer);
    }

    @Override
    public boolean bound() {
        if (AS_FLOAT_EXP) {
            return super.bound();
        }

        return _exp.bound();
    }

    @Override
    public double calcCoeffs(Map map, double factor) throws NonLinearExpression {
        return _exp.calcCoeffs(map, factor);
    }

    @Override
    public boolean isLinear() {
        return _exp.isLinear();
    }

    public double max() {
        return _exp.max();
    }

    // public void removeValue(double value) throws Failure
    // {
    // int valueI = (int)value;
    //
    // if (valueI == value)
    // _exp.removeValue(valueI);
    // else
    // constrainer().fail("removeValue() for FloatExpIntExp: not an integer
    // value: "+value);
    // }

    public double min() {
        return _exp.min();
    }

    @Override
    public void onMaskChange() {
        _observer.publish(publisherMask(), _exp);
    }

    public void setMax(double max) throws Failure {
        if (max >= _exp.max()) {
            return;
        }

        if (max < _exp.min()) {
            constrainer().fail("FloatExpIntExp.setMax()");
        }

        // Truncate to negative infinity. Conversion to int Ok: max in
        // [min()..max())
        int maxI = (int) Math.floor(max);

        _exp.setMax(maxI);
    }

    public void setMin(double min) throws Failure {
        if (min <= _exp.min()) {
            return;
        }

        if (min > _exp.max()) {
            constrainer().fail("FloatExpIntExp.setMin()");
        }

        // Truncate to positive infinity. Conversion to int Ok: min in
        // (min()..max()].
        int minI = (int) Math.ceil(min);

        _exp.setMin(minI);
    }

    public void setValue(double value) throws Failure {
        int valueI = (int) value;

        if (valueI == value) {
            _exp.setValue(valueI);
        } else {
            constrainer().fail("FloatExpIntExp.setValue(): bad integer value: " + value);
        }
    }

    @Override
    public String toString() {
        return "Float(" + _exp + ")";
    }

    /**
     * Float expression is bound to the mean value in the interval associated
     * with this expresion.
     */
    @Override
    public double value() throws Failure {
        if (AS_FLOAT_EXP) {
            return super.value();
        }

        return _exp.value();
    }

} // ~ FloatExpIntExp
