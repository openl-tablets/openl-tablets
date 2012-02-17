package org.openl.ie.constrainer.impl;

import java.util.Map;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Constraint;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.IntBoolExp;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpConst;
import org.openl.ie.constrainer.NonLinearExpression;


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
 * A generic implementation of the IntExp interface.
 */
public abstract class IntExpImpl extends ExpressionImpl implements IntExp {
    static public String domainToString(int min, int max) {
        return min == max ? "[" + min + "]" : "[" + min + ".." + max + "]";
    }

    public IntExpImpl(Constrainer constrainer) {
        this(constrainer, "");
    }

    public IntExpImpl(Constrainer constrainer, String name) {
        super(constrainer, name);
    }

    public IntExp abs() {
        if (min() >= 0) {
            return this;
        }

        if (max() <= 0) {
            return neg();
        }

        // return new IntExpAbs(this);
        return getIntExp(IntExpAbs.class, this);
    }

    public FloatExp add(double value) {
        return asFloat().add(value);
    }

    public FloatExp add(FloatExp exp) {
        // return new FloatExpAddExp(this.asFloat(),exp);
        return getFloatExp(FloatExpAddExp.class, asFloat(), exp);
    }

    public IntExp add(int value) {
        // return new IntExpAddValue(this,value);
        return getIntExp(IntExpAddValue.class, this, value);
    }

    public IntExp add(IntExp exp) {
        // return new IntExpAddExp(this,exp);
        return getIntExp(IntExpAddExp.class, this, exp);
    }

    public FloatExp asFloat() {
        // return new FloatExpIntExp(this);
        return getFloatExp(FloatExpIntExp.class, this);
    }

    public IntExp bitAnd(IntExp exp) {
        return getIntExp(IntExpBitAndExp.class, this, exp);
    }

    public boolean bound() {
        return min() == max();
    }

    public double calcCoeffs(Map map) throws NonLinearExpression {
        return calcCoeffs(map, 1);
    }

    public double calcCoeffs(Map map, double factor) throws NonLinearExpression {
        throw new NonLinearExpression(this);
    }

    public boolean contains(int value) // better to be redefined in subclasses
    {
        return (value >= min() && value <= max());
    }

    public FloatExp div(double c) {
        return asFloat().div(c);
    }

    /**
     * changed by SV 02.13.03 to fixed invalid behaviour
     */
    public IntExp div(int c) {
        if (c == 0) {
            throw new IllegalArgumentException("div(IntExp exp, int value): value == 0");
        } else if (c == 1) {
            return this;
        } else if (c == -1) {
            return neg();
        } else {
            return getIntExp(IntExpDivValue.class, this, c);
        }
    }

    public IntExp div(IntExp divisor) throws Failure {
        divisor.removeValue(0);
        if (divisor == this) {
            return new IntExpConst(constrainer(), 1);
        }
        if (divisor.bound()) {
            int value;
            try {
                value = divisor.value();
            } catch (Exception e) {
                value = 0;
            }
            return this.div(value);
        }

        return getIntExp(IntExpDivExp.class, this, divisor);
    }

    public String domainToString() {
        return domainToString(min(), max());
    }

    public IntBoolExp eq(double value) {
        return asFloat().eq(value);
    }

    public IntBoolExp eq(int value) {
        // return new IntBoolExpEqValue(this, value);
        return getIntBoolExp(IntBoolExpEqValue.class, this, value);
    }

    public IntBoolExp eq(IntExp exp) {
        // return new IntBoolExpEqExp(this, exp);
        return getIntBoolExp(IntBoolExpEqExp.class, this, exp);
    }

    public Constraint equals(FloatExp exp) {
        return exp.equals(this);
    }

    public Constraint equals(FloatExp exp, double value) {
        return exp.equals(this, value);
    }

    public Constraint equals(int value) // this == value
    {
        return new ConstraintExpEqualsValue(this, value);
    }

    public Constraint equals(IntExp exp) // this == exp
    {
        return new ConstraintExpEqualsExp(this, exp);
    }

    public Constraint equals(IntExp exp, int value) // this == exp + value
    {
        return new ConstraintExpEqualsExp(this, exp, value);
    }

    public IntBoolExp ge(double value) {
        return asFloat().ge(value);
    }

    public IntBoolExp ge(int value) {
        return gt(value - 1);
    }

    public IntBoolExp ge(IntExp exp) {
        // return new IntBoolExpLessExp(exp, this, 1);
        return getIntBoolExp(IntBoolExpLessExp.class, exp, this, 1);
        // return gt(exp.sub(1));
    }

    public IntBoolExp gt(double value) {
        return asFloat().gt(value);
    }

    public IntBoolExp gt(int value) {
        // return gt(new IntExpConst(_constrainer, value));
        return gt(getIntExp(IntExpConst.class, value));
    }

    public IntBoolExp gt(IntExp exp) {
        // return new IntBoolExpLessExp(exp, this);
        return getIntBoolExp(IntBoolExpLessExp.class, exp, this);
    }

    /*
     * IntExp mul_2(IntExp exp) { if (exp.min() >= 0) return mulPositive(exp);
     *
     * if (exp.max() <= 0) return mulNegative(exp);
     *
     * int c = -exp.min();
     *
     * return mulPositive(exp.add(c)).add(this.mul(-c)); }
     *
     * IntExp mulPositive(IntExp exp) {
     *
     * if (this.min() >= 0) // return new IntExpMulExpPP(this,exp); return
     * getIntExp(IntExpMulExpPP.class, this, exp);
     *
     * if (this.max() <= 0) // return (new IntExpMulExpPP(neg(),exp)).neg();
     * return getIntExp(IntExpMulExpPP.class, neg(), exp).neg();
     *
     * int c = - min();
     *  // return new IntExpMulExpPP(add(c),exp).add(exp.mul(-c)); return
     * getIntExp(IntExpMulExpPP.class, add(c), exp).add(exp.mul(-c)); }
     *
     * IntExp mulNegative(IntExp exp) { return mulPositive(exp.neg()).neg(); }
     *
     */

    public boolean isLinear() {
        return false;
    }

    public void iterateDomain(IntExp.IntDomainIterator it) throws Failure {
        for (int i = min(); i <= max(); ++i) {
            if (contains(i)) {
                boolean res = it.doSomethingOrStop(i);
                if (!res) {
                    return;
                }
            }
        }
    }

    public IntBoolExp le(double value) {
        return asFloat().le(value);
    }

    public IntBoolExp le(int value) {
        return lt(value + 1);
    }

    public IntBoolExp le(IntExp exp) {
        // return new IntBoolExpLessExp(this,exp,1);
        return getIntBoolExp(IntBoolExpLessExp.class, this, exp, 1);
        // return lt(exp.add(1));
    }

    public Constraint less(int value) {
        return new ConstraintExpLessValue(this, value);
    }

    public Constraint less(IntExp exp) {
        return new ConstraintExpLessExp(this, exp, 1);
    }

    public Constraint lessOrEqual(FloatExp exp) {
        return exp.moreOrEqual(this);
    }

    public Constraint lessOrEqual(int value) {
        return less(value + 1);
    }

    public Constraint lessOrEqual(IntExp exp) {
        return new ConstraintExpLessExp(this, exp, 0);
    }

    public IntBoolExp lt(double value) {
        return asFloat().lt(value);
    }

    public IntBoolExp lt(int value) {
        // return lt(new IntExpConst(_constrainer, value));
        return lt(getIntExp(IntExpConst.class, value));
    }

    public IntBoolExp lt(IntExp exp) {
        // return new IntBoolExpLessExp(this,exp);
        return getIntBoolExp(IntBoolExpLessExp.class, this, exp);
    }

    public FloatExp mod(double c) {
        throw new UnsupportedOperationException("mod");
    }

    public IntExp mod(int c) {
        throw new UnsupportedOperationException("mod");
    }

    public Constraint more(int value) {
        return new ConstraintExpMoreValue(this, value);
    }

    public Constraint more(IntExp exp) {
        return new ConstraintExpLessExp(exp, this, 1);
    }

    public Constraint moreOrEqual(FloatExp exp) {
        return exp.lessOrEqual(this);
    }

    public Constraint moreOrEqual(int value) {
        return more(value - 1);
    }

    /*
     * public final IntEventBool createIntEventBool(boolean b) { IntEventBool e =
     * (IntEventBool)_constrainer._int_event_bool_factory.getElement();
     * e.exp(this); e.init(b); return e; }
     */

    /*
     * public final IntEventAddExp createIntEventAddExp(IntEvent e, IntExp
     * second) { IntEventAddExp exp =
     * (IntEventAddExp)_constrainer._int_event_add_exp_factory.getElement();
     * exp.exp(this); exp.init(e, second); return exp; }
     *
     * public final IntEventAddValue createIntEventAddValue(IntEvent e, int
     * value) { IntEventAddValue exp =
     * (IntEventAddValue)_constrainer._int_event_add_value_factory.getElement();
     * exp.exp(this); exp.init(e, value); return exp; }
     *
     * public final IntEventMulPositiveValue
     * createIntEventMulPositiveValue(IntEvent e, int value) {
     * IntEventMulPositiveValue exp =
     * (IntEventMulPositiveValue)_constrainer._int_event_mul_positive_value_factory.getElement();
     * exp.exp(this); exp.init(e, value); return exp; }
     *
     *
     * public final IntEventOpposite createIntEventOpposite(IntEvent e) {
     * IntEventOpposite exp =
     * (IntEventOpposite)_constrainer._int_event_opposite_factory.getElement();
     * exp.exp(this); exp.init(e); return exp; }
     *
     * public final IntEventDomain createIntEventDomain(IntDomainHistory hist) {
     * IntEventDomain exp =
     * (IntEventDomain)_constrainer._int_event_domain_factory.getElement();
     * exp.exp(this); exp.init(hist); return exp; }
     *
     */

    public Constraint moreOrEqual(IntExp exp) {
        return new ConstraintExpLessExp(exp, this, 0);
    }

    public FloatExp mul(double c) {
        return asFloat().mul(c);
    }

    public FloatExp mul(FloatExp exp) {
        return exp.mul(this);
    }

    public IntExp mul(int c) {
        // Debug.print("Create " + this + " * " + Integer.toString(c));
        if (c == 1) {
            return this;
        }

        if (c > 0) {
            // return new IntExpMultiplyPositive(this,c);
            return getIntExp(IntExpMultiplyPositive.class, this, c);
        }

        if (c == 0) {
            // return new IntExpConst(constrainer(),0);
            return getIntExp(IntExpConst.class, 0);
        }

        // if ( c < 0 )
        return neg().mul(-c);
    }

    public IntExp mul(IntExp exp) {
        if (exp.bound()) {
            return mul(exp.max());
        }

        if (exp == this) {
            return sqr();
        }

        return mul_1(exp);
    }

    IntExp mul_1(IntExp exp) {
        // return new IntExpMulExp(this,exp);
        return getIntExp(IntExpMulExp.class, this, exp);
    }

    public IntBoolExp ne(double value) {
        return asFloat().ne(value);
    }

    public IntBoolExp ne(int value) {
        // return ne(new IntExpConst(_constrainer, value));
        return ne(getIntExp(IntExpConst.class, value));
    }

    public IntBoolExp ne(IntExp exp) {
        // return (new IntBoolExpEqExp(this, exp)).not();
        return getIntBoolExp(IntBoolExpEqExp.class, this, exp).not();
    }

    public IntExp neg() {
        // return new IntExpOpposite(this);
        return getIntExp(IntExpOpposite.class, this);
    }

    public FloatExp pow(double value) throws Failure {
        return asFloat().pow(value);
    }

    public IntExp pow(int value) {
        switch (value) {
            case 0:
                // return new IntExpConst(constrainer(),1);
                return getIntExp(IntExpConst.class, 1);
            case 1:
                return this;
            case 2:
                return sqr();
            default:
                if (value > 0) {
                    // return new IntExpPowIntValue(this,value);
                    return getIntExp(IntExpPowIntValue.class, this, value);
                } else // if(value < 0)
                {
                    throw new IllegalArgumentException("pow(IntExp exp, int value): value < 0");
                }
        }
    }

    public IntExp pow(IntExp pow_exp) throws Failure {
        if (pow_exp.max() < 0) {
            throw new IllegalArgumentException("pow(IntExp exp, IntExp pow_exp): pow_exp < 0");
        } else {
            pow_exp.setMin(0);
            return getIntExp(IntExpPowIntExp.class, this, pow_exp);
        }
    }

    // Only variables should implement propagation.
    @Override
    public void propagate() throws Failure {
    }

    public IntExp rangeViolation(int rangeMin, int rangeMax) {
        // // An implementation as expression.
        // // ((this < rangeMin)*(rangeMin - this) + (this > rangeMax)*(this -
        // rangeMax))</code>.
        // IntExp v1 = lt(rangeMin).mul(sub(rangeMin).neg());
        // IntExp v2 = gt(rangeMax).mul(sub(rangeMax));
        // return v1.add(v2);

        // return new IntExpRangeViolation(this,rangeMin,rangeMax);
        return (IntExp) getExpression(IntExpRangeViolation.class, new Object[] { this, new Integer(rangeMin),
                new Integer(rangeMax) });
    }

    public void removeRange(int min, int max) throws Failure {
        /*
         * commented by SV 02.06.03 by SV due to domain improvements if(min >
         * max) throw new IllegalArgumentException("removeRange: min > max");
         *
         * if(min <= min()) { setMin(max + 1); } else if(max >= max()) {
         * setMax(min - 1); } else // min() < min <= max < max() {
         * removeRangeInternal(min,max); }
         */
        removeRangeInternal(min, max); // added by SV 02.06.03 by SV due to
                                        // domain improvements
    }

    protected void removeRangeInternal(int min, int max) throws Failure {
    }

    public void removeValue(int value) throws Failure {
        int min, max;
        if (value == (min = min())) {
            setMin(min + 1);
        } else if (value == (max = max())) {
            setMax(max - 1);
        } else {
            removeValueInternal(value);
        }

    }

    protected void removeValueInternal(int value) throws Failure {
    }

    public void setValue(int value) throws Failure {
        setMin(value);
        setMax(value);
    }

    public int size() // better to be redefined in subclasses
    {
        return max() - min() + 1;
    }

    public IntExp sqr() {
        // return new IntExpSqr(this);
        return getIntExp(IntExpSqr.class, this);
    }

    public FloatExp sub(double value) {
        return add(-value);
    }

    public FloatExp sub(FloatExp exp) {
        return add(exp.neg());
    }

    public IntExp sub(int value) {
        return add(-value);
    }

    public IntExp sub(IntExp exp) {
        return add(exp.neg());
    }

    /**
     * Returns a String representation of this object.
     *
     * @return a String representation of this object.
     */
    @Override
    public String toString() {
        return name() + domainToString();
    }

    public boolean valid() {
        return min() <= max();
    }

    public int value() throws Failure {
        int min = min();

        if (min != max()) {
            _constrainer.fail("Attempt to get value of an unbound expression" + this);
        }

        return min;
    }

    public int valueUnsafe() {
        return min();
    }
} // ~IntExpImpl
