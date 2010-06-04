package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.Observer;
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

public class IntExpDivExp extends IntExpImpl {
    class IntExpDivExpObserver extends Observer {

        @Override
        public Object master() {
            return IntExpDivExp.this;
        }

        @Override
        public int subscriberMask() {
            return MIN | MAX | VALUE;
        }

        @Override
        public String toString() {
            return "IntExpMulExpObserver: " + _numerator + " x " + _denominator;
        }

        @Override
        public void update(Subject subject, EventOfInterest event) throws Failure {
            if ((_denominator.min() == 0) || (_denominator.max() == 0)) {
                _denominator.removeValue(0);
            }
            _quotient.setMin(calc_min());
            _quotient.setMax(calc_max());
        }

    } // ~ IntExpMulExpObserver
    private IntExp _numerator, _denominator;
    private Observer _observer;

    private IntVar _quotient;

    public IntExpDivExp(IntExp numerator, IntExp denominator) {
        super(numerator.constrainer());
        _numerator = numerator;
        _denominator = denominator;

        if (constrainer().showInternalNames()) {
            _name = "(" + _numerator.name() + "/" + _denominator.name() + ")";
        }

        _observer = new IntExpDivExpObserver();
        _numerator.attachObserver(_observer);
        _denominator.attachObserver(_observer);

        int trace = 0;
        _quotient = constrainer().addIntVarTraceInternal(calc_min(), calc_max(), "div", IntVar.DOMAIN_PLAIN, trace);

    }

    @Override
    public void attachObserver(Observer observer) {
        super.attachObserver(observer);
        _quotient.attachObserver(observer);
    }

    private int calc_max() {
        int min1 = _numerator.min();
        int max1 = _numerator.max();
        int min2 = _denominator.min();
        int max2 = _denominator.max();
        if (max2 > 0 && min2 < 0) {
            return Math.max(Math.abs(min1), Math.abs(min2));
        }
        return Math.max(Math.max(min1 / min2, min1 / max2), Math.max(max1 / min2, max1 / max2));
    }

    private int calc_min() {
        int min1 = _numerator.min();
        int max1 = _numerator.max();
        int min2 = _denominator.min();
        int max2 = _denominator.max();
        if (max2 > 0 && min2 < 0) {
            return -Math.max(Math.abs(min1), Math.abs(min2));
        }
        return Math.min(Math.min(min1 / min2, min1 / max2), Math.min(max1 / min2, max1 / max2));
    }

    @Override
    public void detachObserver(Observer observer) {
        super.detachObserver(observer);
        _quotient.detachObserver(observer);
    }

    public int max() {
        return _quotient.max();
    }

    public int min() {
        return _quotient.min();
    }

    @Override
    public void name(String name) {
        super.name(name);
        _quotient.name(name);
    }

    @Override
    public void reattachObserver(Observer observer) {
        super.reattachObserver(observer);
        _quotient.reattachObserver(observer);
    }

    public void setMax(int max) throws Failure {
        if (max >= max()) {
            return;
        }

        _quotient.setMax(max);

        int dmin = _denominator.min();
        int dmax = _denominator.max();
        int remainder = Math.max(Math.abs(dmin), Math.abs(dmax)) - 1;

        int nmin = IntCalc.productMin(max, min(), dmin, dmax);
        _numerator.setMin(nmin - remainder);

        int nmax = IntCalc.productMax(max, min(), dmin, dmax);
        _numerator.setMax(nmax + remainder);
    }

    public void setMin(int min) throws Failure {
        if (min <= min()) {
            return;
        }

        _quotient.setMin(min);

        int dmin = _denominator.min();
        int dmax = _denominator.max();
        int remainder = Math.max(Math.abs(dmin), Math.abs(dmax)) - 1;

        int nmin = IntCalc.productMin(min, max(), dmin, dmax);
        _numerator.setMin(nmin - remainder);

        int nmax = IntCalc.productMax(min, max(), dmin, dmax);
        _numerator.setMax(nmax + remainder);
    }

    @Override
    public void setValue(int val) throws Failure {
        setMin(val);
        setMax(val);
    }

}