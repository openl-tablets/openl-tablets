package org.openl.ie.constrainer;

import org.openl.ie.constrainer.impl.FloatExpImpl;

/**
 * An implementation of the constant floating-point expression. Many methods from FloatExpImpl are overloaded with
 * optimized implementation.
 */
public final class FloatExpConst extends FloatExpImpl {
    private double _const;

    public FloatExpConst(Constrainer constrainer, double c) {
        super(constrainer, Double.toString(c));
        _const = c;
    }

    @Override
    public FloatExp add(double value) {
        // return new FloatExpConst(constrainer(),_const + value);
        return getFloatExp(FloatExpConst.class, _const + value);
    }

    @Override
    public boolean bound() {
        return true;
    }

    @Override
    public boolean isLinear() {
        return true;
    }

    @Override
    public Constraint lessOrEqual(double value) {
        return new ConstraintConst(constrainer(), _const <= value);
    }

    public double max() {
        return _const;
    }

    public double min() {
        return _const;
    }

    @Override
    public Constraint moreOrEqual(double value) {
        return new ConstraintConst(constrainer(), _const >= value);
    }

    @Override
    public void propagate() throws Failure {
    }

    public void setMax(double max) throws Failure {
        if (max < _const) {
            constrainer().fail("max<const");
        }
    }

    public void setMin(double min) throws Failure {
        if (min > _const) {
            constrainer().fail("min>const");
        }
    }

    public void setValue(double value) throws Failure {
        if (value != _const) {
            constrainer().fail("value!=const");
        }
    }

    @Override
    public String toString() {
        return "[" + _const + "]";
    }

    @Override
    public double value() throws Failure {
        return _const;
    }

} // ~FloatExpConst
