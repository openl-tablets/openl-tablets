package org.openl.ie.constrainer.impl;

import java.util.Map;

import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntVar;
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
//
//: IntExpMulExpPP.java
//
/**
 * An implementation of the expression: <code>(IntExp1 * IntExp2)</code> where
 * both IntExp1 and IntExp2 are non-negative.
 */
public final class IntExpMulExpPP extends IntExpImpl {
    class ExpMultiplyPositiveObserver extends Observer {

        ExpMultiplyPositiveObserver() {
            // super(event_map);
        }

        @Override
        public Object master() {
            return IntExpMulExpPP.this;
        }

        @Override
        public int subscriberMask() {
            return MIN | MAX | VALUE;
        }

        @Override
        public String toString() {
            return "ExpMulExpPPObserver: " + _exp1 + " x " + _exp2;
        }

        @Override
        public void update(Subject subject, EventOfInterest event) throws Failure {

            // System.out.println(this);

            int mask = event.type();

            if ((mask & MAX) != 0) {
                _product.setMax(calc_max());
            }
            if ((mask & MIN) != 0) {
                _product.setMin(calc_min());
            }
        }

    } // ~ ExpMultiplyPositiveObserver

    private IntExp _exp1, _exp2;

    private Observer _observer;

    private IntVar _product;

    public IntExpMulExpPP(IntExp exp1, IntExp exp2) {
        super(exp1.constrainer());
        // if ( value <= 0 )
        // abort("negative value in IntExpMultiplyPositive");
        _exp1 = exp1;
        _exp2 = exp2;

        if (constrainer().showInternalNames()) {
            _name = "(" + exp1.name() + "*" + exp2.name() + ")";
        }

        int trace = 0;
        _product = constrainer().addIntVarTraceInternal(calc_min(), calc_max(), "mul", IntVar.DOMAIN_PLAIN, trace);

        _observer = new ExpMultiplyPositiveObserver();

        _exp1.attachObserver(_observer);
        _exp2.attachObserver(_observer);
    }

    @Override
    public void attachObserver(Observer observer) {
        super.attachObserver(observer);
        _product.attachObserver(observer);
    }

    int calc_max() {
        return _exp1.max() * _exp2.max();
    }

    int calc_min() {
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

    @Override
    public boolean contains(int value) {
        /*
         * if (value % _value != 0) return false; return
         * _exp.contains(value/_value);
         */
        return super.contains(value);
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

    public int max() {
        return _product.max();
    }

    public int min() {
        return _product.min();
    }

    @Override
    public void onMaskChange() {
        // _observer.publish(publisherMask(), _exp1);
        // _observer.publish(publisherMask(), _exp2);
    }

    @Override
    public void reattachObserver(Observer observer) {
        super.reattachObserver(observer);
        _product.reattachObserver(observer);
    }

    @Override
    public void removeValue(int value) throws Failure {
        // if (value % _value != 0)
        // return;
        // _exp.removeValue(value/_value);
    }

    public void setMax(int max) throws Failure {
        if (max >= max()) {
            return;
        }

        _product.setMax(max);

        // System.out.println("++++ Set max: " + max + " in " + this);
        // System.out.println("setmax: " + max + " in " + this);

        if (max < min()) {
            constrainer().fail("Mul PP Set Max");
        }

        int Pmax = max;
        int Min, Max, delta, q;

        // (a/b)*b + (a%b) = a - java spec
        // 5/3 = 1(2)
        // -5/3 = -1(-2)
        // -5/-3 = 1(-2)
        // 5/-3 = -1(2)

        // P = (71, 90), val = 10, var=(8,9)
        // P = (70, 89), val = 10, var=(7,8)
        // P = (-80, -61), val = 10, var = (-8, -7)
        // P = (-79, -60), val = 10, var = (-7, -6)

        int v = _exp2.min();

        if (v != 0) {
            q = Pmax / v;
            delta = Pmax - q * v;
            Max = delta >= 0 ? q : q - 1;

            // System.out.println("setmax 1: " + Max + " in " + this);
            _exp1.setMax(Max); // may fail
        }

        v = _exp1.min();

        if (v != 0) {
            q = Pmax / v;
            delta = Pmax - q * v;
            Max = delta >= 0 ? q : q - 1;

            // System.out.println("setmax 2: " + Max + " in " + this);
            _exp2.setMax(Max); // may fail
        }

    }

    /*
     * public int size() // not necessary the true { return _exp1.size() *
     * _exp2.size(); }
     */

    public void setMin(int min) throws Failure {
        if (min <= min()) {
            return;
        }

        _product.setMin(min);

        // System.out.println("setmin: " + min + " in " + this);

        if (min > max()) {
            constrainer().fail("Mul PP Set Min");
        }

        int Pmin = min;
        int Min, Max, delta, q;

        // (a/b)*b + (a%b) = a - java spec
        // 5/3 = 1(2)
        // -5/3 = -1(-2)
        // -5/-3 = 1(-2)
        // 5/-3 = -1(2)

        // P = (71, 90), val = 10, var=(8,9)
        // P = (70, 89), val = 10, var=(7,8)
        // P = (-80, -61), val = 10, var = (-8, -7)
        // P = (-79, -60), val = 10, var = (-7, -6)

        // V > 0
        // min: Pmin = q * V + d, V > d > -V
        // Xmin * V >= q * V + d > (Xmin - 1) * V
        // Xmin > q - 1
        // a) d > 0
        // Xmin * V > q * V
        // Xmin > q >> Xmin = q + 1
        //
        // b) d <= 0
        // q > Xmin - 1
        // q+1 > Xmin >> Xmin = q

        int v = _exp2.max();

        if (v != 0) {
            q = Pmin / v;
            delta = Pmin - q * v;
            Min = delta > 0 ? q + 1 : q;

            _exp1.setMin(Min); // may fail
        }
        // System.out.println("setmin 1: " + Min + " in " + this);

        v = _exp1.max();

        if (v != 0) {
            q = Pmin / v;
            delta = Pmin - q * v;
            Min = delta > 0 ? q + 1 : q;

            // System.out.println("setmin 2: " + Min + " in " + this);
            _exp2.setMin(Min); // may fail
        }

    }

    @Override
    public void setValue(int value) throws Failure {
        // if (value % _value != 0)
        // constrainer().fail("invalid setValue in IntExpMultiplyPositiveImpl");
        // _exp.setValue(value/_value);
        setMin(value);
        setMax(value);
    }

    @Override
    public int value() throws Failure {
        return _product.value();
    }

} // ~IntExpMulPP
