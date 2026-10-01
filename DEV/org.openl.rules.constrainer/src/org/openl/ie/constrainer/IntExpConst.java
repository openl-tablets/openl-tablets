package org.openl.ie.constrainer;

/**
 * An integer constant.
 */
final class IntExpConst extends IntExp {

    private final int value;

    IntExpConst(Constrainer constrainer, int value) {
        super(constrainer);
        this.value = value;
    }

    @Override
    int min() {
        return value;
    }

    @Override
    int max() {
        return value;
    }

    @Override
    void setMin(int min) throws Failure {
        if (min > value) {
            throw new Failure();
        }
    }

    @Override
    void setMax(int max) throws Failure {
        if (max < value) {
            throw new Failure();
        }
    }
}
