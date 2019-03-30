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

    public FloatExp add(double value) {
        // optimization for the case when the Expression is bounded
        if (bound()) {
            return getFloatExp(FloatExpConst.class, max() + value);
        }

        return getFloatExp(FloatExpAddValue.class, this, value);
    }

    public boolean bound() {
        return FloatCalc.eq(min(), max());
    }

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

    public IntBoolExp eq(double value) {
        return eq(getFloatExp(FloatExpConst.class, value));
    }

    public IntBoolExp eq(FloatExp exp) {
        return getIntBoolExp(IntBoolExpFloatEqExp.class, this, exp);
    }

    public IntBoolExp eq(int value) {
        return eq((double) value);
    }

    public IntBoolExp ge(double value) {
        return ge(getFloatExp(FloatExpConst.class, value));
    }

    public IntBoolExp ge(FloatExp exp) {
        return getIntBoolExp(IntBoolExpFloatLessExp.class, exp, this);
    }

    public IntBoolExp ge(int value) {
        return ge((double) value);
    }

    public IntBoolExp gt(double value) {
        return ge(value);
    }

    public IntBoolExp gt(FloatExp exp) {
        return ge(exp);
    }

    public IntBoolExp gt(int value) {
        return gt((double) value);
    }

    public boolean isLinear() {
        return false;
    }

    public IntBoolExp le(double value) {
        return le(getFloatExp(FloatExpConst.class, value));
    }

    public IntBoolExp le(FloatExp exp) {
        return getIntBoolExp(IntBoolExpFloatLessExp.class, this, exp);
    }

    public IntBoolExp le(int value) {
        return le((double) value);
    }

    public Constraint lessOrEqual(double value) {
        return new ConstraintFloatExpLessValue(this, value);
    }

    public IntBoolExp lt(double value) {
        return le(value);
    }

    public IntBoolExp lt(FloatExp exp) {
        return le(exp);
    }

    public IntBoolExp lt(int value) {
        return lt((double) value);
    }

    public FloatExp mod(double c) {
        throw new UnsupportedOperationException("mod");
    }

    public FloatExp mod(int c) {
        throw new UnsupportedOperationException("mod");
    }

    public Constraint moreOrEqual(double value) {
        return new ConstraintFloatExpMoreValue(this, value);
    }

    // Only variables should implement propagation.
    @Override
    public void propagate() throws Failure {
    }

    public void removeRange(double min, double max) throws Failure {
        if (min < min()) {
            setMin(max);
        } else if (max > max()) {
            setMax(min);
        }
    }

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

    public double value() throws Failure {
        /*
         * if (!bound ()) { constrainer ().fail ("Attempt to get value of the unbound float expresion " + this); }
         */
        return (min() + max()) / 2;
    }
}