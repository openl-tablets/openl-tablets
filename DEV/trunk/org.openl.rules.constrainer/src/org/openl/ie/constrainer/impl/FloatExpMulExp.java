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
 * An implementation of the expression: <code>(FloatExp1 * FloatExp2)</code>.
 */
public final class FloatExpMulExp extends FloatExpImpl {
    /**
     * Calculation for general exp1 and exp2
     */
    final class CalcGeneral extends FloatExpMulExpCalc {
        private FloatExp _exp1, _exp2;

        public CalcGeneral(FloatExp exp1, FloatExp exp2) {
            _exp1 = exp1;
            _exp2 = exp2;
        }

        @Override
        public void createProduct() {
            double min1 = _exp1.min();
            double max1 = _exp1.max();
            double min2 = _exp2.min();
            double max2 = _exp2.max();
            double min = FloatCalc.productMin(min1, max1, min2, max2);
            double max = FloatCalc.productMax(min1, max1, min2, max2);
            createProductVar(min, max);
        }

        @Override
        public double max() {
            return FloatCalc.productMax(_exp1.min(), _exp1.max(), _exp2.min(), _exp2.max());
        }

        @Override
        public double min() {
            return FloatCalc.productMin(_exp1.min(), _exp1.max(), _exp2.min(), _exp2.max());
        }

        @Override
        public void setMax(double max) throws Failure {
            FloatCalc.productSetMax(max, _exp1, _exp2);
            FloatCalc.productSetMax(max, _exp2, _exp1);
        }

        @Override
        public void setMin(double min) throws Failure {
            FloatCalc.productSetMin(min, _exp1, _exp2);
            FloatCalc.productSetMin(min, _exp2, _exp1);
        }

        @Override
        public void updateFromObserver() throws Failure {
            double min1 = _exp1.min();
            double max1 = _exp1.max();
            double min2 = _exp2.min();
            double max2 = _exp2.max();
            double min = FloatCalc.productMin(min1, max1, min2, max2);
            double max = FloatCalc.productMax(min1, max1, min2, max2);
            updateProductVar(min, max);
        }

    } // ~CalcGeneral
    /**
     * Calculation for exp1 <= 0
     */
    final class CalcN extends FloatExpMulExpCalc {
        private FloatExp _exp1, _exp2;

        public CalcN(FloatExp exp1, FloatExp exp2) {
            _exp1 = exp1;
            _exp2 = exp2;
        }

        @Override
        public void createProduct() {
            double min1 = _exp1.min();
            double max1 = _exp1.max();
            double min2 = _exp2.min();
            double max2 = _exp2.max();
            double min = FloatCalc.productMinN(min1, max1, max2);
            double max = FloatCalc.productMaxN(min1, max1, min2);
            createProductVar(min, max);
        }

        @Override
        public double max() {
            return FloatCalc.productMaxN(_exp1.min(), _exp1.max(), _exp2.min());
        }

        @Override
        public double min() {
            return FloatCalc.productMinN(_exp1.min(), _exp1.max(), _exp2.max());
        }

        @Override
        public void setMax(double max) throws Failure {
            double min1 = _exp1.min();
            double max1 = _exp1.max();
            FloatCalc.productSetMaxN(max, min1, max1, _exp2);
            FloatCalc.productSetMax(max, _exp2, _exp1);
        }

        @Override
        public void setMin(double min) throws Failure {
            double min1 = _exp1.min();
            double max1 = _exp1.max();
            FloatCalc.productSetMinN(min, min1, max1, _exp2);
            FloatCalc.productSetMin(min, _exp2, _exp1);
        }

        @Override
        public void updateFromObserver() throws Failure {
            double min1 = _exp1.min();
            double max1 = _exp1.max();
            double min2 = _exp2.min();
            double max2 = _exp2.max();
            double min = FloatCalc.productMinN(min1, max1, max2);
            double max = FloatCalc.productMaxN(min1, max1, min2);
            updateProductVar(min, max);
        }

    } // ~CalcN
    /**
     * Calculation for exp1 <= 0 && exp2 <= 0
     */
    final class CalcNN extends FloatExpMulExpCalc {
        private FloatExp _exp1, _exp2;

        public CalcNN(FloatExp exp1, FloatExp exp2) {
            _exp1 = exp1;
            _exp2 = exp2;
        }

        @Override
        public double max() {
            return _exp1.min() * _exp2.min();
        }

        @Override
        public double min() {
            return _exp1.max() * _exp2.max();
        }

        @Override
        public void setMax(double max) throws Failure {
            FloatCalc.productSetMaxN(max, _exp1.min(), _exp1.max(), _exp2);
            FloatCalc.productSetMaxN(max, _exp2.min(), _exp2.max(), _exp1);
        }

        @Override
        public void setMin(double min) throws Failure {
            FloatCalc.productSetMinN(min, _exp1.min(), _exp1.max(), _exp2);
            FloatCalc.productSetMinN(min, _exp2.min(), _exp2.max(), _exp1);
        }

    } // ~CalcNN
    /**
     * Calculation for exp1 >= 0
     */
    final class CalcP extends FloatExpMulExpCalc {
        private FloatExp _exp1, _exp2;

        public CalcP(FloatExp exp1, FloatExp exp2) {
            _exp1 = exp1;
            _exp2 = exp2;
        }

        @Override
        public void createProduct() {
            double min1 = _exp1.min();
            double max1 = _exp1.max();
            double min2 = _exp2.min();
            double max2 = _exp2.max();
            double min = FloatCalc.productMinP(min1, max1, min2);
            double max = FloatCalc.productMaxP(min1, max1, max2);
            createProductVar(min, max);
        }

        @Override
        public double max() {
            return FloatCalc.productMaxP(_exp1.min(), _exp1.max(), _exp2.max());
        }

        @Override
        public double min() {
            return FloatCalc.productMinP(_exp1.min(), _exp1.max(), _exp2.min());
        }

        @Override
        public void setMax(double max) throws Failure {
            double min1 = _exp1.min();
            double max1 = _exp1.max();
            FloatCalc.productSetMaxP(max, min1, max1, _exp2);
            FloatCalc.productSetMax(max, _exp2, _exp1);
        }

        @Override
        public void setMin(double min) throws Failure {
            double min1 = _exp1.min();
            double max1 = _exp1.max();
            FloatCalc.productSetMinP(min, min1, max1, _exp2);
            FloatCalc.productSetMin(min, _exp2, _exp1);
        }

        @Override
        public void updateFromObserver() throws Failure {
            double min1 = _exp1.min();
            double max1 = _exp1.max();
            double min2 = _exp2.min();
            double max2 = _exp2.max();
            double min = FloatCalc.productMinP(min1, max1, min2);
            double max = FloatCalc.productMaxP(min1, max1, max2);
            updateProductVar(min, max);
        }

    } // ~CalcP

    /**
     * Calculation for exp1 >= 0 && exp2 <= 0
     */
    final class CalcPN extends FloatExpMulExpCalc {
        private FloatExp _exp1, _exp2;

        public CalcPN(FloatExp exp1, FloatExp exp2) {
            _exp1 = exp1;
            _exp2 = exp2;
        }

        @Override
        public double max() {
            return _exp1.min() * _exp2.max();
        }

        @Override
        public double min() {
            return _exp1.max() * _exp2.min();
        }

        @Override
        public void setMax(double max) throws Failure {
            FloatCalc.productSetMaxP(max, _exp1.min(), _exp1.max(), _exp2);
            FloatCalc.productSetMaxN(max, _exp2.min(), _exp2.max(), _exp1);
        }

        @Override
        public void setMin(double min) throws Failure {
            FloatCalc.productSetMinP(min, _exp1.min(), _exp1.max(), _exp2);
            FloatCalc.productSetMinN(min, _exp2.min(), _exp2.max(), _exp1);
        }

    } // ~CalcPN

    /**
     * Calculation for exp1 >= 0 && exp2 >= 0
     */
    final class CalcPP extends FloatExpMulExpCalc {

        private FloatExp _exp1, _exp2;

        public CalcPP(FloatExp exp1, FloatExp exp2) {
            _exp1 = exp1;
            _exp2 = exp2;
        }

        @Override
        public double max() {
            return _exp1.max() * _exp2.max();
        }

        @Override
        public double min() {
            return _exp1.min() * _exp2.min();
        }

        @Override
        public void setMax(double max) throws Failure {
            FloatCalc.productSetMaxP(max, _exp1.min(), _exp1.max(), _exp2);
            FloatCalc.productSetMaxP(max, _exp2.min(), _exp2.max(), _exp1);
        }

        @Override
        public void setMin(double min) throws Failure {
            FloatCalc.productSetMinP(min, _exp1.min(), _exp1.max(), _exp2);
            FloatCalc.productSetMinP(min, _exp2.min(), _exp2.max(), _exp1);
        }

    } // ~CalcPP

    /**
     * Various calculations depending on exp1 and exp2 signs.
     */
    abstract class FloatExpMulExpCalc {
        // May be overriden where min/max more optimal to calculate
        // simultaneously.
        public void createProduct() {
            createProductVar(min(), max());
        }

        abstract public double max();

        abstract public double min();

        abstract public void setMax(double max) throws Failure;

        abstract public void setMin(double min) throws Failure;

        // May be overriden where min/max more optimal to calculate
        // simultaneously.
        public void updateFromObserver() throws Failure {
            updateProductVar(min(), max());
        }
    }

    final class FloatExpMulExpObserver extends Observer {

        @Override
        public Object master() {
            return FloatExpMulExp.this;
        }

        @Override
        public int subscriberMask() {
            return MIN | MAX | VALUE;
        }

        @Override
        public String toString() {
            return "FloatExpMulExpObserver: " + _exp1 + " x " + _exp2;
        }

        @Override
        public void update(Subject subject, EventOfInterest event) throws Failure {
            _calc.updateFromObserver();
        }

    } // ~ FloatExpMulExpObserver

    private FloatExp _exp1, _exp2;

    private Observer _observer;

    private FloatVar _product;

    private FloatExpMulExpCalc _calc;

    public FloatExpMulExp(FloatExp exp1, FloatExp exp2) {
        super(exp1.constrainer(), "");
        if (constrainer().showInternalNames()) {
            _name = exp1.name() + "*" + exp2.name();
        }
        _exp1 = exp1;
        _exp2 = exp2;

        createCalc();

        _calc.createProduct();

        _observer = new FloatExpMulExpObserver();

        _exp1.attachObserver(_observer);
        _exp2.attachObserver(_observer);
    }

    @Override
    public void attachObserver(Observer observer) {
        super.attachObserver(observer);
        _product.attachObserver(observer);
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

    void createCalc() {
        if (_exp1.min() >= 0) {
            if (_exp2.min() >= 0) {
                _calc = new CalcPP(_exp1, _exp2);
            } else if (_exp2.max() <= 0) {
                _calc = new CalcPN(_exp1, _exp2);
            } else {
                _calc = new CalcP(_exp1, _exp2);
            }
        } else if (_exp1.max() <= 0) {
            if (_exp2.min() >= 0) {
                _calc = new CalcPN(_exp2, _exp1); // NP
            } else if (_exp2.max() <= 0) {
                _calc = new CalcNN(_exp1, _exp2);
            } else {
                _calc = new CalcN(_exp1, _exp2);
            }
        } else {
            if (_exp2.min() >= 0) {
                _calc = new CalcP(_exp2, _exp1);
            } else if (_exp2.max() <= 0) {
                _calc = new CalcN(_exp2, _exp1);
            } else {
                _calc = new CalcGeneral(_exp1, _exp2);
            }
        }
    }

    void createProductVar(double min, double max) {
        int trace = 0;
        _product = constrainer().addFloatVarTraceInternal(min, max, _name, trace);
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
    public void name(String name) {
        super.name(name);
        _product.name(name);
    }

    @Override
    public void reattachObserver(Observer observer) {
        super.reattachObserver(observer);
        _product.reattachObserver(observer);
    }

    public void setMax(double max) throws Failure {
        // System.out.println("setmax: " + max + " in " + this);

        if (max >= max()) {
            return;
        }

        _product.setMax(max);

        // if (max < min())
        // {
        // constrainer().fail("FloatExpMulExp.setMax(): max < min()");
        // }

        _calc.setMax(max);
    }

    public void setMin(double min) throws Failure {
        // System.out.println("setmin: " + min + " in " + this);

        if (min <= min()) {
            return;
        }

        _product.setMin(min);

        // if (min > max())
        // {
        // constrainer().fail("FloatExpMulExp.setMin(): min > max()");
        // }

        _calc.setMin(min);
    }

    public void setValue(double value) throws Failure {
        setMin(value);
        setMax(value);
    }

    void updateProductVar(double min, double max) throws Failure {
        _product.setMin(min);
        _product.setMax(max);
    }

} // ~FloatExpMulExp
