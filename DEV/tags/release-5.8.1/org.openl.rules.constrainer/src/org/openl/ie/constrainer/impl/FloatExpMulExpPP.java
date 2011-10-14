package org.openl.ie.constrainer.impl;

import java.util.Map;

import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.FloatVar;
import org.openl.ie.constrainer.NonLinearExpression;
import org.openl.ie.constrainer.Observer;
import org.openl.ie.constrainer.Subject;


///////////////////////////////////////////////////////////////////////////////
/*
 * Copyright Exigen Group 1998, 1999, 2000
 * 320 Amboy Ave., Metuchen, NJ, 08840, USA, www.exigengroup.com
 *
 * The copyright to the computer program(s) herein
 * is the property of Exigen Group, USA. All rights reserved.
 * The program(s) may be used and/or copied only with
 * the written permission of Exigen Group
 * or in accordance with the terms and conditions
 * stipulated in the agreement/contract under which
 * the program(s) have been supplied.
 */
///////////////////////////////////////////////////////////////////////////////
/**
 * An implementation of the expression: <code>(FloatExp1 * FloatExp2)</code>
 * where both expressions are non-negative.
 */
public final class FloatExpMulExpPP extends FloatExpImpl {
    class FloatExpMulExpPPObserver extends ExpressionObserver {

        FloatExpMulExpPPObserver() {
            super(event_map);
        }

        @Override
        public Object master() {
            return FloatExpMulExpPP.this;
        }

        @Override
        public String toString() {
            return "FloatExpMulExpPPObserver: " + _exp1 + " x " + _exp2;
        }

        @Override
        public void update(Subject subject, EventOfInterest event) throws Failure {
            _product.setMax(calc_max());
            _product.setMin(calc_min());
            // checkMe("FloatExpMulExpPPObserver:");
        }

    } // ~ FloatExpMulExpPPObserver

    static final private int[] event_map = { MIN, MIN, MAX, MAX, };

    private FloatExp _exp1, _exp2;

    private ExpressionObserver _observer;

    private FloatVar _product;

    public FloatExpMulExpPP(FloatExp exp1, FloatExp exp2) {
        super(exp1.constrainer(), "");// exp1.name()+"*"+exp2.name());
        _exp1 = exp1;
        _exp2 = exp2;

        int trace = 0;
        _product = constrainer().addFloatVarTraceInternal(calc_min(), calc_max(), "mul", trace);

        _observer = new FloatExpMulExpPPObserver();

        _exp1.attachObserver(_observer);
        _exp2.attachObserver(_observer);
    }

    @Override
    public void attachObserver(Observer observer) {
        super.attachObserver(observer);
        _product.attachObserver(observer);
    }

    double calc_max() {
        return _exp1.max() * _exp2.max();
    }

    double calc_min() {
        return _exp1.min() * _exp2.min();
    }

    @Override
    public double calcCoeffs(Map map, double factor) throws NonLinearExpression {
        if (_exp1.bound()) {
            return _exp2.calcCoeffs(map, factor * _exp1.max());
        }
        if (_exp2.bound()) {
            return _exp1.calcCoeffs(map, factor * _exp2.max());
        }
        throw new NonLinearExpression(this);
    }

    private boolean checkMe(String s) {
        if (calc_max() != _product.max() || calc_min() != _product.min()) {
            System.out.println(s + "[" + calc_min() + ".." + calc_max() + "]!=" + this);
            return false;
        }
        return true;
    }

    @Override
    public void detachObserver(Observer observer) {
        super.detachObserver(observer);
        _product.detachObserver(observer);
    }

    @Override
    public boolean isLinear() {
        if (!((_exp1.bound()) || (_exp2.bound()))) {
            return false;
        }
        return (_exp1.isLinear() && _exp2.isLinear());
    }

    public double max() {
        return _product.max();
    }

    public double min() {
        return _product.min();
    }

    @Override
    public void onMaskChange() {
        _observer.publish(publisherMask(), _exp1);
        _observer.publish(publisherMask(), _exp2);
    }

    @Override
    public void reattachObserver(Observer observer) {
        super.reattachObserver(observer);
        _product.reattachObserver(observer);
    }

    public void setMax(double max) throws Failure {
        if (max >= max()) {
            return;
        }

        // System.out.println("setmax: " + max + " in " + this);

        if (max < min()) {
            constrainer().fail("FloatExpMulExpPP.setMax(): max < min()");
        }

        // max(max(x1)) <= max(max/any(x2)) = max/min(x2)

        double v, Max;

        v = _exp2.min();
        if (v > 0) {
            Max = max / v;
            // System.out.println("setmax 1: " + Max + " in " + this);
            _exp1.setMax(Max); // may fail
        }

        v = _exp1.min();
        if (v > 0) {
            Max = max / v;
            // System.out.println("setmax 2: " + Max + " in " + this);
            _exp2.setMax(Max); // may fail
        }

    }

    public void setMin(double min) throws Failure {
        if (min <= min()) {
            return;
        }

        // System.out.println("setmin: " + min + " in " + this);

        if (min > max()) {
            constrainer().fail("FloatExpMulExpPP.setMin(): min > max()");
        }

        // min(min(x1)) >= min(min/any(x2)) = min/max(x2)

        double Min, v;

        v = _exp2.max();
        if (v > 0) {
            Min = min / v;
            // System.out.println("setmin 1: " + Min + " in " + this);
            _exp1.setMin(Min); // may fail
        }

        v = _exp1.max();
        if (v > 0) {
            Min = min / v;
            // System.out.println("setmin 2: " + Min + " in " + this);
            _exp2.setMin(Min); // may fail
        }

    }

    public void setValue(double value) throws Failure {
        setMin(value);
        setMax(value);
    }

    @Override
    public String toString() {
        return (_exp1 + " x " + _exp2);
    }

} // ~FloatExpMulPP
