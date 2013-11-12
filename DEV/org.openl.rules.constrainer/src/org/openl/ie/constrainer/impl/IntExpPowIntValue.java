package org.openl.ie.constrainer.impl;

import java.util.Map;

import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.NonLinearExpression;
import org.openl.ie.constrainer.Observer;
import org.openl.ie.constrainer.Subject;


/**
 * An implementation of the expression: <code>pow(IntExp,value)</code> for the
 * intgeger value. Value assumed to be integer and > 0.
 * <p>
 * Examples:
 *
 * <pre>
 * IntVar var = constrainer.addIntVar(min, max, name);
 * IntExp exp = var.pow(3);
 * </pre>
 */
public final class IntExpPowIntValue extends IntExpImpl {
    class IntExpPowIntValueObserver extends ExpressionObserver {

        IntExpPowIntValueObserver() {
            super(event_map);
        }

        @Override
        public Object master() {
            return IntExpPowIntValue.this;
        }

        @Override
        public int subscriberMask() {
            return MIN | MAX | VALUE;
        }

        @Override
        public String toString() {
            return "IntExpPowIntValueObserver: " + "pow(" + _exp + "," + _value + ")";
        }

        @Override
        public void update(Subject exp, EventOfInterest event) throws Failure {
            // IntEvent e = (IntEvent) event;

            int min = _exp.min(), max = _exp.max();
            _result.setMin(calc_min(min, max, _value));
            _result.setMax(calc_max(min, max, _value));
        }
    } // ~ IntExpPowIntValueObserver
    static final private int[] event_map = { MIN | MAX, MIN, MIN | MAX, MAX, VALUE, VALUE, REMOVE, REMOVE };
    private IntExp _exp;
    private int _value;

    private IntVar _result;

    private ExpressionObserver _observer;

    static int calc_max(int min, int max, int value) {
        if (value % 2 == 0) {
            return Math.max((int) Math.pow(min, value), (int) Math.pow(max, value));
        } else {
            return (int) Math.pow(max, value);
        }
    }

    static int calc_min(int min, int max, int value) {
        if (value % 2 == 0) {
            // min >= 0 && max >= 0
            if (min >= 0) {
                return (int) Math.pow(min, value);
            } else if (max <= 0) {
                return (int) Math.pow(max, value);
            // min < 0 && max > 0
            } else {
                return 0;
            }
        } else {
            return (int) Math.pow(min, value);
        }
    }

    public IntExpPowIntValue(IntExp exp, int value) {
        super(exp.constrainer());
        _exp = exp;
        _value = value;

        if (constrainer().showInternalNames()) {
            _name = "IlcPower(" + exp.name() + "," + value + ")";
            // _name = "("+exp.name()+"**"+value+")";
        }

        _observer = new IntExpPowIntValueObserver();
        _exp.attachObserver(_observer);
        int min = _exp.min(), max = _exp.max();
        int trace = 0;
        int domain = IntVar.DOMAIN_PLAIN;
        _result = constrainer().addIntVarTraceInternal(calc_min(min, max, _value), calc_max(min, max, _value), "pow",
                domain, trace);
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

    public int max() {
        return _result.max();
    }

    public int min() {
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

    public void setMax(int max) throws Failure {
        if ((_value % 2) == 0) {
            if (max < 0) {
                constrainer().fail("pow(exp,value).setMax(): max < 0 for even value");
            // ???
            }
        } else {
            // ???
        }
    }

    public void setMin(int min) throws Failure {
        if ((_value % 2) == 0) {
            if (min <= 0) {
                return;
            // ???
            }
        } else {
            // ???
        }
    }

    @Override
    public void setValue(int value) throws Failure {
        setMin(value);
        setMax(value);
    }

    @Override
    public String toString() {
        return "pow(" + _exp + "," + _value + ")" + domainToString();
    }

} // ~IntExpPowIntValue
