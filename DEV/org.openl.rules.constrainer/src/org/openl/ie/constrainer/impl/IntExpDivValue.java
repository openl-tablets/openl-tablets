package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.Subject;

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2002
 * </p>
 * <p>
 * Company:
 * </p>
 *
 * @author unascribed
 * @version 1.0
 */

public class IntExpDivValue extends IntExpImpl {
    class IntExpDivValueObserver extends ExpressionObserver {
        IntExpDivValueObserver() {
            super(event_map);
        }

        @Override
        public Object master() {
            return IntExpDivValue.this;
        }

        @Override
        public String toString() {
            return "IntExpDivExpObserver: " + "(" + _dividend.name() + "/" + _divisor + ")";
        }

        @Override
        public void update(Subject exp, EventOfInterest event) throws Failure {
            _quotient.setMin(calc_min());
            _quotient.setMax(calc_max());
        }
    }
    static final private int[] event_map = { MIN | MAX, MIN, MIN | MAX, MAX, VALUE, VALUE };
    private IntExp _dividend;

    private int _divisor;

    private IntExp _quotient;

    private IntExpDivValueObserver _observer;

    /**
     * NB: divisor should be different from -1, 0, 1
     *
     * @param dividend
     * @param divisor
     */
    public IntExpDivValue(IntExp dividend, int divisor) {
        super(dividend.constrainer());
        _dividend = dividend;
        _divisor = divisor;

        if (constrainer().showInternalNames()) {
            _name = "(" + dividend.name() + "/" + divisor + ")";
        }

        _observer = new IntExpDivValueObserver();
        _dividend.attachObserver(_observer);

        int trace = 0;
        int domain = IntVar.DOMAIN_PLAIN;
        _quotient = constrainer().addIntVarTraceInternal(calc_min(), calc_max(), "div", domain, trace);
    }

    int calc_max() {
        if (_divisor > 0) {
            return _dividend.max() / _divisor;
        } else {
            return _dividend.min() / _divisor;
        }
    }

    int calc_min() {
        if (_divisor > 0) {
            return _dividend.min() / _divisor;
        } else {
            return _dividend.max() / _divisor;
        }
    }

    public int max() {
        return _quotient.max();
    }

    public int min() {
        return _quotient.min();
    }

    public void setMax(int max) throws Failure {
        _quotient.setMax(max);

        if (_divisor > 0) {
            if (max < 0) {
                _dividend.setMax(max * _divisor);
            } else { // max >= 0
                // division of lesser values with remainder lead to the same
                // result
                _dividend.setMax((max + 1) * _divisor - 1);
            }
        } else { // divisor < 0
            if (max < 0) {
                _dividend.setMin(max * _divisor);
            } else { // max >= 0
                // division of bigger values with remainder lead to the same
                // result
                _dividend.setMin((max + 1) * _divisor - 1);
            }
        }
    }

    public void setMin(int min) throws Failure {
        _quotient.setMin(min);

        if (_divisor > 0) {
            if (min > 0) {
                _dividend.setMin(min * _divisor);
            } else { // min <= 0
                // division of lesser values with remainder lead to the same
                // result
                _dividend.setMin((min - 1) * _divisor + 1);
            }
        } else { // divisor < 0
            if (min > 0) {
                _dividend.setMax(min * _divisor);
            } else { // min <= 0
                // division of bigger values with remainder lead to the same
                // result
                _dividend.setMax((min - 1) * _divisor + 1);
            }
        }
    }
}