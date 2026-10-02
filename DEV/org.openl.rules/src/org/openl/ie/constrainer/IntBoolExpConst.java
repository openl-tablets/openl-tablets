package org.openl.ie.constrainer;

/**
 * The boolean constant: a boolean variable bound from the start. The expressions made of it are simplified.
 */
final class IntBoolExpConst extends IntBoolVar {

    IntBoolExpConst(Constrainer constrainer, boolean value) {
        super(constrainer);
        min = value ? 1 : 0;
        max = min;
    }

    @Override
    public IntExp add(int value) {
        return new IntExpConst(constrainer, min + value);
    }

    @Override
    public IntExp add(IntExp exp) {
        return exp.add(min);
    }

    @Override
    public IntBoolExp and(boolean value) {
        return isTrue() ? constrainer.constant(value) : this;
    }

    @Override
    public IntBoolExp and(IntBoolExp exp) {
        return isTrue() ? exp : this;
    }

    @Override
    public IntBoolExp or(boolean value) {
        return isTrue() ? this : constrainer.constant(value);
    }

    @Override
    public IntBoolExp or(IntBoolExp exp) {
        return isTrue() ? this : exp;
    }
}
