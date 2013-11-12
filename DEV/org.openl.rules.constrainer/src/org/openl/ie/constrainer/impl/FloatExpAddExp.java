package org.openl.ie.constrainer.impl;

import java.util.Map;

import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.FloatVar;
import org.openl.ie.constrainer.NonLinearExpression;
import org.openl.ie.constrainer.Observer;
import org.openl.ie.constrainer.Subject;


/**
 * An implementation of the expression: <code>(FloatExp1 + FloatExp2)</code>.
 */
public final class FloatExpAddExp extends FloatExpImpl {
    class ExpAddExpObserver extends Observer {
        @Override
        public Object master() {
            return FloatExpAddExp.this;
        }

        @Override
        public int subscriberMask() {
            return MIN | MAX | VALUE;
        }

        @Override
        public String toString() {
            return "ExpAddExpObserver: ";
        }

        @Override
        public void update(Subject exp, EventOfInterest event) throws Failure {
            // FloatEvent e = (FloatEvent) event;

            // int type = e.type();
            // if ((type & MIN) != 0)
            _sum.setMin(calc_min());

            // if ((type & MAX) != 0)
            _sum.setMax(calc_max());

        }

    } // ~ ExpAddExpObserver
    private FloatExp _exp1;
    private FloatExp _exp2;

    private Observer _observer;

    private FloatVar _sum;

    public FloatExpAddExp(FloatExp exp1, FloatExp exp2) {
        super(exp1.constrainer(), "");// exp.name()+"+"+value);

        _exp1 = exp1;
        _exp2 = exp2;

        int trace = 0;
        _sum = constrainer().addFloatVarTraceInternal(calc_min(), calc_max(), "sum", trace);

        _exp1.attachObserver(_observer = new ExpAddExpObserver());
        _exp2.attachObserver(_observer);
    }

    @Override
    public void attachObserver(Observer observer) {
        super.attachObserver(observer);
        _sum.attachObserver(observer);
    }

    public double calc_max() {
        return _exp1.max() + _exp2.max();
    }

    public double calc_min() {
        return _exp1.min() + _exp2.min();
    }

    @Override
    public double calcCoeffs(Map map, double factor) throws NonLinearExpression {
        return (_exp1.calcCoeffs(map, factor) + _exp2.calcCoeffs(map, factor));
    }

    @Override
    public void detachObserver(Observer observer) {
        super.detachObserver(observer);
        _sum.detachObserver(observer);
    }

    /*
     * public double value() throws Failure { return _sum.value(); }
     */
    @Override
    public boolean isLinear() {
        return (_exp1.isLinear() && _exp2.isLinear());
    }

    public double max() {
        return _sum.max();
    }

    public double min() {
        return _sum.min();
    }

    @Override
    public void reattachObserver(Observer observer) {
        super.reattachObserver(observer);
        _sum.reattachObserver(observer);
    }

    public void setMax(double max) throws Failure {
        if (max >= _sum.max()) {
            return;
        }

        double max1 = max - _exp2.min();
        if (max1 < _exp1.max()) {
            _exp1.setMax(max1);
        }
        double max2 = max - _exp1.min();
        if (max2 < _exp2.max()) {
            _exp2.setMax(max2);
        }
    }

    public void setMin(double min) throws Failure {

        if (min <= _sum.min()) {
            return;
        }

        double min1 = min - _exp2.max();
        if (min1 > _exp1.min()) {
            _exp1.setMin(min1);
        }
        double min2 = min - _exp1.max();
        if (min2 > _exp2.min()) {
            _exp2.setMin(min2);
        }
    }

    public void setValue(double value) throws Failure {
        setMin(value);
        setMax(value);
    }

    // public String toString()
    // {
    // return "(" + _exp1 + "+" + _exp2 + ")" ;
    // }

    /*
     * static final class FloatEventAddExp extends FloatEvent {
     *
     * static ReusableFactory _factory = new ReusableFactory() { Reusable
     * createNewElement() { return new FloatEventAddExp(); }
     *  };
     *
     * static FloatEventAddExp getEvent(FloatEvent event, FloatExp exp) {
     * FloatEventAddExp ev = (FloatEventAddExp) _factory.getElement();
     * ev.init(event, exp); return ev; }
     *
     *
     *
     *
     * int _type; FloatExp _second; FloatEvent _event;
     *
     * public String name() { return "Event AddExp"; }
     *
     *
     *
     * public void init(FloatEvent e, FloatExp second) { _event = e; _second =
     * second; _type = e.type(); if (!_second.bound()) _type &= ~(REMOVE |
     * VALUE); }
     *
     * public int type() { return _type; }
     *
     *
     *
     *
     * public double min() { return _event.min() + _second.min(); }
     *
     * public double max() { return _event.max() + _second.max(); }
     *
     * public double oldmin() { return _event.oldmin() + _second.min(); }
     *
     *
     * public double oldmax() { return _event.oldmax() + _second.max(); }
     *  } // ~FloatEventAddExp
     */

} // ~FloatExpAddExp
