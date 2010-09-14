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
 * An implementation of the expression: <code>pow(FloatExp,value)</code> for
 * the intgeger value. Value assumed to be integer and > 0.
 * <p>
 * Examples:
 *
 * <pre>
 * FloatVar var = constrainer.addFloatVar(min, max, name);
 * FloatExp exp = var.pow(3);
 * </pre>
 */
public final class FloatExpPowIntValue extends FloatExpImpl {
    class FloatExpPowIntValueObserver extends ExpressionObserver {

        FloatExpPowIntValueObserver() {
            super(event_map);
        }

        @Override
        public Object master() {
            return FloatExpPowIntValue.this;
        }

        @Override
        public int subscriberMask() {
            return MIN | MAX | VALUE;
        }

        @Override
        public String toString() {
            return "FloatExpPowIntValueObserver: " + "pow(" + _exp + "," + _value + ")";
        }

        @Override
        public void update(Subject exp, EventOfInterest event) throws Failure {
            // FloatEvent e = (FloatEvent) event;

            double min = _exp.min(), max = _exp.max();
            _result.setMin(calc_min(min, max, _value));
            _result.setMax(calc_max(min, max, _value));
        }
    } // ~ FloatExpPowIntValueObserver
    static final private int[] event_map = { MIN | MAX, MIN, MIN | MAX, MAX, VALUE, VALUE, REMOVE, REMOVE };
    private FloatExp _exp;
    private int _value;

    private FloatVar _result;

    private ExpressionObserver _observer;

    static double calc_max(double min, double max, int value) {
        if (value % 2 == 0) {
            return Math.max(Math.pow(min, value), Math.pow(max, value));
        } else {
            return Math.pow(max, value);
        }
    }

    static double calc_min(double min, double max, int value) {
        if (value % 2 == 0) {
            // min >= 0 && max >= 0
            if (min >= 0) {
                return Math.pow(min, value);
            } else if (max <= 0) {
                return Math.pow(max, value);
            // min < 0 && max > 0
            } else {
                return 0;
            }
        } else {
            return Math.pow(min, value);
        }
    }

    public FloatExpPowIntValue(FloatExp exp, int value) {
        super(exp.constrainer(), "");// "pow("+exp.name()"+","+value+")");
        _exp = exp;
        _value = value;
        _observer = new FloatExpPowIntValueObserver();
        _exp.attachObserver(_observer);
        double min = _exp.min(), max = _exp.max();
        int trace = 0;
        _result = constrainer().addFloatVarTraceInternal(calc_min(min, max, _value), calc_max(min, max, _value), "pow",
                trace);
    }

    @Override
    public void attachObserver(Observer observer) {
        super.attachObserver(observer);
        _result.attachObserver(observer);
    }

    @Override
    public double calcCoeffs(Map map, double factor) throws NonLinearExpression {
        if (!((_value == 0) || (_value == 1))) {
            throw new NonLinearExpression(this);
        }
        if (_value == 0) {
            return 1;
        } else {
            return _exp.calcCoeffs(map, factor);
        }
    }

    @Override
    public void detachObserver(Observer observer) {
        super.detachObserver(observer);
        _result.detachObserver(observer);
    }

    @Override
    public boolean isLinear() {
        return (_exp.isLinear() && ((_value == 1) || (_value == 0)));
    }

    public double max() {
        return _result.max();
    }

    public double min() {
        return _result.min();
    }

    @Override
    public void onMaskChange() {
        _observer.publish(publisherMask(), _exp);
    }

    @Override
    public void reattachObserver(Observer observer) {
        super.reattachObserver(observer);
        _result.reattachObserver(observer);
    }

    public void setMax(double max) throws Failure {
        if ((_value % 2) == 0) {
            if (max < 0) {
                constrainer().fail("pow(exp,value).setMax(): max < 0 for even value");
            // ???
            }
        } else {
            // ???
        }
    }

    public void setMin(double min) throws Failure {
        if ((_value % 2) == 0) {
            if (min <= 0) {
                return;
            // ???
            }
        } else {
            // ???
        }
    }

    public void setValue(double value) throws Failure {
        setMin(value);
        setMax(value);
    }

    @Override
    public String toString() {
        return "pow(" + _exp + "," + _value + ")" + domainToString();
    }

} // ~FloatExpPowIntValue
