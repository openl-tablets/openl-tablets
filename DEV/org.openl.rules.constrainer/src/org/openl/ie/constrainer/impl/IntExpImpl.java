package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.*;

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

    @Override
    public IntExp add(int value) {
        // return new IntExpAddValue(this,value);
        return getIntExp(IntExpAddValue.class, this, value);
    }

    @Override
    public IntExp add(IntExp exp) {
        // return new IntExpAddExp(this,exp);
        return getIntExp(IntExpAddExp.class, this, exp);
    }

    @Override
    public boolean bound() {
        return min() == max();
    }

    @Override
    public boolean contains(int value) // better to be redefined in subclasses
    {
        return (value >= min() && value <= max());
    }

    @Override
    public String domainToString() {
        return domainToString(min(), max());
    }

    @Override
    public IntBoolExp eq(int value) {
        // return new IntBoolExpEqValue(this, value);
        return getIntBoolExp(IntBoolExpEqValue.class, this, value);
    }

    @Override
    public Constraint equals(int value) // this == value
    {
        return new ConstraintExpEqualsValue(this, value);
    }

    @Override
    public Constraint equals(IntExp exp) // this == exp
    {
        return new ConstraintExpEqualsExp(this, exp);
    }

    @Override
    public IntBoolExp ge(int value) {
        return gt(value - 1);
    }

    @Override
    public IntBoolExp gt(int value) {
        // return gt(new IntExpConst(_constrainer, value));
        return gt(getIntExp(IntExpConst.class, value));
    }

    @Override
    public IntBoolExp gt(IntExp exp) {
        // return new IntBoolExpLessExp(exp, this);
        return getIntBoolExp(IntBoolExpLessExp.class, exp, this);
    }

    @Override
    public boolean isLinear() {
        return false;
    }

    @Override
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

    @Override
    public IntBoolExp le(int value) {
        return lt(value + 1);
    }

    @Override
    public IntBoolExp lt(int value) {
        // return lt(new IntExpConst(_constrainer, value));
        return lt(getIntExp(IntExpConst.class, value));
    }

    @Override
    public IntBoolExp lt(IntExp exp) {
        // return new IntBoolExpLessExp(this,exp);
        return getIntBoolExp(IntBoolExpLessExp.class, this, exp);
    }

    /*
     * public final IntEventBool createIntEventBool(boolean b) { IntEventBool e =
     * (IntEventBool)_constrainer._int_event_bool_factory.getElement(); e.exp(this); e.init(b); return e; }
     */

    /*
     * public final IntEventAddExp createIntEventAddExp(IntEvent e, IntExp second) { IntEventAddExp exp =
     * (IntEventAddExp)_constrainer._int_event_add_exp_factory.getElement(); exp.exp(this); exp.init(e, second); return
     * exp; }
     *
     * public final IntEventAddValue createIntEventAddValue(IntEvent e, int value) { IntEventAddValue exp =
     * (IntEventAddValue)_constrainer._int_event_add_value_factory.getElement(); exp.exp(this); exp.init(e, value);
     * return exp; }
     *
     * public final IntEventMulPositiveValue createIntEventMulPositiveValue(IntEvent e, int value) {
     * IntEventMulPositiveValue exp =
     * (IntEventMulPositiveValue)_constrainer._int_event_mul_positive_value_factory.getElement(); exp.exp(this);
     * exp.init(e, value); return exp; }
     *
     *
     * public final IntEventOpposite createIntEventOpposite(IntEvent e) { IntEventOpposite exp =
     * (IntEventOpposite)_constrainer._int_event_opposite_factory.getElement(); exp.exp(this); exp.init(e); return exp;
     * }
     *
     * public final IntEventDomain createIntEventDomain(IntDomainHistory hist) { IntEventDomain exp =
     * (IntEventDomain)_constrainer._int_event_domain_factory.getElement(); exp.exp(this); exp.init(hist); return exp; }
     *
     */

    IntExp mul_1(IntExp exp) {
        // return new IntExpMulExp(this,exp);
        return getIntExp(IntExpMulExp.class, this, exp);
    }

    // Only variables should implement propagation.
    @Override
    public void propagate() throws Failure {
    }

    @Override
    public void removeRange(int min, int max) throws Failure {
        /*
         * commented by SV 02.06.03 by SV due to domain improvements if(min > max) throw new
         * IllegalArgumentException("removeRange: min > max");
         *
         * if(min <= min()) { setMin(max + 1); } else if(max >= max()) { setMax(min - 1); } else // min() < min <= max <
         * max() { removeRangeInternal(min,max); }
         */
        removeRangeInternal(min, max); // added by SV 02.06.03 by SV due to
        // domain improvements
    }

    protected void removeRangeInternal(int min, int max) throws Failure {
    }

    @Override
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

    @Override
    public void setValue(int value) throws Failure {
        setMin(value);
        setMax(value);
    }

    @Override
    public int size() // better to be redefined in subclasses
    {
        return max() - min() + 1;
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

    @Override
    public int value() throws Failure {
        int min = min();

        if (min != max()) {
            _constrainer.fail("Attempt to get value of an unbound expression" + this);
        }

        return min;
    }

    @Override
    public int valueUnsafe() {
        return min();
    }
} // ~IntExpImpl
