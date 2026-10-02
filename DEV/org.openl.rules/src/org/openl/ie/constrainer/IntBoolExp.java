package org.openl.ie.constrainer;

/**
 * A constrained boolean expression: an integer expression with the domain within [0..1], where 0 is false and 1 is
 * true.
 * <p>
 * OpenL binds the operators {@code &&} and {@code ||} of the condition formulas to the methods {@code and} and
 * {@code or} by their names.
 */
public abstract class IntBoolExp extends IntExp {

    IntBoolExp(Constrainer constrainer) {
        super(constrainer);
    }

    IntBoolExp(Constrainer constrainer, String name) {
        super(constrainer, name);
    }

    /**
     * Returns the expression {@code this && value}.
     */
    public IntBoolExp and(boolean value) {
        return value ? this : constrainer.constant(false);
    }

    /**
     * Returns the expression {@code this && exp}.
     */
    public IntBoolExp and(IntBoolExp exp) {
        return new IntBoolExpAnd(this, exp);
    }

    /**
     * Returns the expression {@code this || value}.
     */
    public IntBoolExp or(boolean value) {
        return value ? constrainer.constant(true) : this;
    }

    /**
     * Returns the expression {@code this || exp}.
     */
    public IntBoolExp or(IntBoolExp exp) {
        return new IntBoolExpOr(this, exp);
    }

    /**
     * Returns whether the expression is true. While the expression is unknown, it is neither true nor false.
     */
    boolean isTrue() {
        return min() == 1;
    }

    /**
     * Returns whether the expression is false. While the expression is unknown, it is neither true nor false.
     */
    boolean isFalse() {
        return max() == 0;
    }

    /**
     * Makes the expression true.
     *
     * @throws Failure if the expression is false
     */
    void setTrue() throws Failure {
        setMin(1);
    }

    /**
     * Makes the expression false.
     *
     * @throws Failure if the expression is true
     */
    void setFalse() throws Failure {
        setMax(0);
    }
}
