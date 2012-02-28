package org.openl.ie.constrainer.impl;

import java.util.Map;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Constraint;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.FloatExpConst;
import org.openl.ie.constrainer.IntBoolExp;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.NonLinearExpression;


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

    // changed 02.20.03 by SV to support calculations with precision
    public FloatExp abs() {
        // optimization for the case when the Expression is bounded
        if (bound()) {
            return getFloatExp(FloatExpConst.class, Math.abs(max()));
        }

        if (min() >= 0.0) {
            return this;
        }
        if (max() <= 0.0) {
            return neg();
        }
        return getFloatExp(FloatExpAbs.class, this);
    }

    public FloatExp add(double value) {
        // optimization for the case when the Expression is bounded
        if (bound()) {
            return getFloatExp(FloatExpConst.class, max() + value);
        }

        return getFloatExp(FloatExpAddValue.class, this, value);
    }

    public FloatExp add(FloatExp exp) {
        // optimization for the case when the Expression is bounded
        if (bound()) {
            return exp.add(max());
        }
        if (exp.bound()) {
            return add(exp.max());
        }
        // -----------------------------------------------------
        return getFloatExp(FloatExpAddExp.class, this, exp);
    }

    public FloatExp add(int value) {
        return add((double) value);
    }

    public FloatExp add(IntExp exp) {
        return add(exp.asFloat());
    }

    public boolean bound() {
        return FloatCalc.eq(min(), max());
    }

    public double calcCoeffs(Map map) throws NonLinearExpression {
        return calcCoeffs(map, 1);
    }

    public double calcCoeffs(Map map, double factor) throws NonLinearExpression {
        throw new NonLinearExpression(this);
    }

    public FloatExp div(double value) {
        // optimization for the case when the Expression is bounded
        if (bound()) {
            return getFloatExp(FloatExpConst.class, max() / value);
        }

        if (value == 1) {
            return this;
        } else if (value == 0) {
            throw new IllegalArgumentException("Division by zero");
        } else {
            return mul(1 / value);
        }
    }

    public FloatExp div(FloatExp exp) {
        // optimization for the case when the Expression is bounded
        if (bound()) {
            return exp.inv().mul(max());
        }

        return mul(exp.inv());
    }

    public FloatExp div(int value) {
        return div((double) value);
    }

    public FloatExp div(IntExp exp) {
        return div(exp.asFloat());
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

    public IntBoolExp eq(IntExp exp) {
        return eq(exp.asFloat());
    }

    public Constraint equals(double value) {
        return new ConstraintFloatExpEqualsValue(this, value);
    }

    public Constraint equals(FloatExp exp) {
        return new ConstraintFloatExpEqualsExp(this, exp);
    }

    public Constraint equals(FloatExp exp, double value) {
        return new ConstraintFloatExpEqualsExp(this, exp, value);
    }

    public Constraint equals(IntExp exp) {
        return equals(exp.asFloat());
    }

    public Constraint equals(IntExp exp, double value) {
        return equals(exp.asFloat(), value);
    }

    public FloatExp exp() {
        // optimization for the case when the Expression is bounded
        if (bound()) {
            return getFloatExp(FloatExpConst.class, Math.exp(max()));
        }

        return getFloatExp(FloatExpExponent.class, this);
    }

    public FloatExp exp(double value) {
        return this.mul(Math.log(value)).exp();
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

    public IntBoolExp ge(IntExp exp) {
        return ge(exp.asFloat());
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

    public IntBoolExp gt(IntExp exp) {
        return gt(exp.asFloat());
    }

    // changed 02.20.03 by SV to support calculations with precision
    public FloatExp inv() {
        if (FloatCalc.eq(max(), 0.0) && FloatCalc.eq(min(), 0.0)) {
            throw new IllegalArgumentException("Division by zero");
        }
        // optimization for the case when the Expression is bounded
        if (bound()) {
            return getFloatExp(FloatExpConst.class, 1.0 / max());
        }
        // ------------------------------------------------------------
        return getFloatExp(FloatExpInverse.class, this);
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

    public IntBoolExp le(IntExp exp) {
        return le(exp.asFloat());
    }

    public Constraint lessOrEqual(double value) {
        return new ConstraintFloatExpLessValue(this, value);
    }

    public Constraint lessOrEqual(FloatExp exp) {
        return new ConstraintFloatExpLessExp(this, exp);
    }

    public Constraint lessOrEqual(IntExp exp) {
        return lessOrEqual(exp.asFloat());
    }

    public FloatExp log() throws Failure {
        if (max() <= 0) {
            throw new IllegalArgumentException("log (): max() < 0");
        } else {
            // optimization for the case when the Expression is bounded
            if (bound()) {
                return getFloatExp(FloatExpConst.class, Math.log(max()));
            }

            setMin(0.0);
            return getFloatExp(FloatExpLog.class, this);
        }
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

    public IntBoolExp lt(IntExp exp) {
        return lt(exp.asFloat());
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

    public Constraint moreOrEqual(FloatExp exp) {
        return new ConstraintFloatExpLessExp(exp, this);
    }

    public Constraint moreOrEqual(IntExp exp) {
        return moreOrEqual(exp.asFloat());
    }

    public FloatExp mul(double value) {
        if (value == 1) {
            return this;
        } else if (value > 0) {
            // add for optimization purposes
            if (bound()) {
                return getFloatExp(FloatExpConst.class, max() * value);
            }
            // -----------------------------------------------------
            return getFloatExp(FloatExpMultiplyPositive.class, this, value);
        } else if (value == 0) {
            return getFloatExp(FloatExpConst.class, 0);
        } else {
            return neg().mul(-value);
        }
    }

    public FloatExp mul(FloatExp exp) {
        if (exp == this) {
            return sqr();
        } else {
            // was added for optimization purposes
            if (bound()) {
                return exp.mul(max());
            }
            if (exp.bound()) {
                return mul(exp.max());
            }
            // ------------------------------------
            return getFloatExp(FloatExpMulExp.class, this, exp);
        }
    }

    public FloatExp mul(int value) {
        return mul((double) value);
    }

    public FloatExp mul(IntExp exp) {
        return mul(exp.asFloat());
    }

    public IntBoolExp ne(double value) {
        return ne(getFloatExp(FloatExpConst.class, value));
    }

    public IntBoolExp ne(FloatExp exp) {
        return getIntBoolExp(IntBoolExpFloatEqExp.class, this, exp).not();
    }

    public IntBoolExp ne(int value) {
        return ne((double) value);
    }

    public IntBoolExp ne(IntExp exp) {
        return ne(exp.asFloat());
    }

    public FloatExp neg() {
        // optimization for the case when the Expression is bounded
        if (bound()) {
            return getFloatExp(FloatExpConst.class, -max());
        }
        // ------------------------------------------------------------
        return getFloatExp(FloatExpOpposite.class, this);
    }

    public FloatExp pow(double value) throws Failure {
        int valueI = (int) value;
        if (valueI == value) {
            return pow(valueI);
        }
        if (min() < 0) {
            throw new IllegalArgumentException("pow (exp, value): exp < 0 for non-integer value");
        }
        if (value > 0) {
            // optimization for the case when the Expression is bounded
            if (bound()) {
                return getFloatExp(FloatExpConst.class, Math.pow(max(), value));
            }
            // ------------------------------------------------------------
            return getFloatExp(FloatExpPowValue.class, this, value);
        } else {
            // pow(x,v) == 1 / pow(x,-v)
            return pow(-value).inv();
        }
    }

    public FloatExp pow(FloatExp exp) throws Failure {
        // pow(x,y) <-> exp(y*log(x))
        if (min() < 0) {
            throw new IllegalArgumentException("pow (exp1, exp2): exp1 < 0 for non-integer value");
        }
        if (exp.bound()) {
            return pow((exp.max() + exp.min()) / 2);
        }
        return log().mul(exp).exp();
    }

    public FloatExp pow(int value) throws Failure {
        switch (value) {
            case 0:
                return getFloatExp(FloatExpConst.class, 1);
            case 1:
                return this;
            case 2:
                return sqr();
            default:
                if (value > 0) {
                    // optimization for the case when the Expression is bounded
                    if (bound()) {
                        return getFloatExp(FloatExpConst.class, Math.pow(max(), value));
                    }
                    // ------------------------------------------------------------
                    return getFloatExp(FloatExpPowIntValue.class, this, value);
                } else {
                    // pow(x,v) == 1 / pow(x,-v)
                    return pow(-value).inv();
                }
        }
    }

    public FloatExp pow(IntExp exp) throws Failure {
        return log().mul(exp.asFloat()).exp();
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

    public FloatExp sqr() {
        // optimization for the case when the Expression is bounded
        if (bound()) {
            return getFloatExp(FloatExpConst.class, max() * max());
        }
        return getFloatExp(FloatExpSqr.class, this);
    }

    public FloatExp sub(double value) {
        return add(-value);
    }

    public FloatExp sub(FloatExp exp) {
        return add(exp.neg());
    }

    public FloatExp sub(int value) {
        return add((double) -value);
    }

    public FloatExp sub(IntExp exp) {
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

    public double value() throws Failure {
        /*
         * if (!bound ()) { constrainer ().fail ("Attempt to get value of the
         * unbound float expresion " + this); }
         */
        return (min() + max()) / 2;
    }
}