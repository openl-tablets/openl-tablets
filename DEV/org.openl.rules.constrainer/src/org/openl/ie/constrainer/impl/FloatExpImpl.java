package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Constraint;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.FloatExpConst;
import org.openl.ie.constrainer.IntBoolExp;

/**
 * A generic implementation of the FloatExp interface.
 */
public abstract class FloatExpImpl extends ExpressionImpl implements FloatExp {
    public FloatExpImpl(Constrainer c) {
        this(c, "");
    }

    public FloatExpImpl(Constrainer c, String name) {
        super(c, name);
    }

    @Override
    public FloatExp add(double value) {
        // optimization for the case when the Expression is bounded
        if (bound()) {
            return getFloatExp(FloatExpConst.class, max() + value);
        }

        return getFloatExp(FloatExpAddValue.class, this, value);
    }

    @Override
    public boolean bound() {
        return FloatCalc.eq(min(), max());
    }

    @Override
    public String domainToString() {
        double min = min();
        double max = max();
        if (min == max) {
            return "[" + min + "]";
        } else if (!bound()) {
            return "[" + min + ".." + max + "]";
        } else {
            return "[" + min + ".." + max + "(" + (min + max) / 2 + ")" + "]";
        }
    }

    @Override
    public IntBoolExp eq(double value) {
        return eq(getFloatExp(FloatExpConst.class, value));
    }

    @Override
    public IntBoolExp eq(FloatExp exp) {
        return getIntBoolExp(IntBoolExpFloatEqExp.class, this, exp);
    }

    @Override
    public IntBoolExp eq(int value) {
        return eq((double) value);
    }

    @Override
    public IntBoolExp ge(double value) {
        return ge(getFloatExp(FloatExpConst.class, value));
    }

    @Override
    public IntBoolExp ge(FloatExp exp) {
        return getIntBoolExp(IntBoolExpFloatLessExp.class, exp, this);
    }

    @Override
    public IntBoolExp ge(int value) {
        return ge((double) value);
    }

    @Override
    public IntBoolExp gt(double value) {
        return ge(value);
    }

    @Override
    public IntBoolExp gt(FloatExp exp) {
        return ge(exp);
    }

    @Override
    public IntBoolExp gt(int value) {
        return gt((double) value);
    }

    @Override
    public boolean isLinear() {
        return false;
    }

    @Override
    public IntBoolExp le(double value) {
        return le(getFloatExp(FloatExpConst.class, value));
    }

    @Override
    public IntBoolExp le(FloatExp exp) {
        return getIntBoolExp(IntBoolExpFloatLessExp.class, this, exp);
    }

    @Override
    public IntBoolExp le(int value) {
        return le((double) value);
    }

    @Override
    public Constraint lessOrEqual(double value) {
        return new ConstraintFloatExpLessValue(this, value);
    }

    @Override
    public IntBoolExp lt(double value) {
        return le(value);
    }

    @Override
    public IntBoolExp lt(FloatExp exp) {
        return le(exp);
    }

    @Override
    public IntBoolExp lt(int value) {
        return lt((double) value);
    }

    @Override
    public FloatExp mod(double c) {
        throw new UnsupportedOperationException("mod");
    }

    @Override
    public FloatExp mod(int c) {
        throw new UnsupportedOperationException("mod");
    }

    @Override
    public Constraint moreOrEqual(double value) {
        return new ConstraintFloatExpMoreValue(this, value);
    }

    // Only variables should implement propagation.
    @Override
    public void propagate() throws Failure {
    }

    @Override
    public void removeRange(double min, double max) throws Failure {
        if (min < min()) {
            setMin(max);
        } else if (max > max()) {
            setMax(min);
        }
    }

    @Override
    public double size() {
        return max() - min();
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
    public double value() throws Failure {
        /*
         * if (!bound ()) { constrainer ().fail ("Attempt to get value of the unbound float expresion " + this); }
         */
        return (min() + max()) / 2;
    }
}