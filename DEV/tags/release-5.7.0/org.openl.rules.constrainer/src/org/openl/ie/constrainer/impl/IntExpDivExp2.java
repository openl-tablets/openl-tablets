package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.Observer;

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

public class IntExpDivExp2 extends IntExpImpl {
    private IntExp _dividend;
    private IntExp _divisor;
    private IntExp _quotient;
    private IntExp _remainder;

    public IntExpDivExp2(IntExp dividend, IntExp divisor) throws Failure {
        super(dividend.constrainer());
        _dividend = dividend;
        _divisor = divisor;

        if (constrainer().showInternalNames()) {
            _name = "(" + dividend.name() + "/" + divisor.name() + ")";
        }

        int trace = 0;
        int domain = IntVar.DOMAIN_PLAIN;
        _quotient = constrainer().addIntVarTraceInternal(calc_min(), calc_max(), "div", domain, trace);
        //
        int tmp = Math.max(Math.abs(_divisor.min()), Math.abs(_divisor.max()));

        _remainder = constrainer().addIntVarTraceInternal((-1 * tmp + 1), (tmp - 1), "div", domain, trace);
        constrainer().postConstraint(_quotient.mul(_divisor).add(_remainder).equals(_dividend));
        constrainer().postConstraint(_quotient.mul(_divisor).abs().lessOrEqual(_dividend.abs()));
        constrainer().postConstraint(_remainder.abs().less(_divisor.abs()));
    }

    @Override
    public void attachObserver(Observer observer) {
        super.attachObserver(observer);
        _quotient.attachObserver(observer);
    }

    private int calc_max() {
        int min1 = _dividend.min();
        int max1 = _dividend.max();
        int min2 = _divisor.min();
        int max2 = _divisor.max();
        if (max2 > 0 && min2 < 0) {
            return Math.max(Math.abs(min1), Math.abs(min2));
        }
        return Math.max(Math.max(min1 / min2, min1 / max2), Math.max(max1 / min2, max1 / max2));
    }

    private int calc_min() {
        int min1 = _dividend.min();
        int max1 = _dividend.max();
        int min2 = _divisor.min();
        int max2 = _divisor.max();
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
    }

    public void setMin(int min) throws Failure {
        if (min <= min()) {
            return;
        }

        _quotient.setMin(min);
    }

    @Override
    public void setValue(int val) throws Failure {
        setMin(val);
        setMax(val);
    }
}