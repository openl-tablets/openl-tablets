package org.openl.rules.dt;

class UnmatchedDtHeader extends DTHeader {
    UnmatchedDtHeader(int[] methodParameterIndexes, String statement, int column, int width) {
        super(methodParameterIndexes, statement, column, width);
    }

    @Override
    boolean isCondition() {
        return false;
    }

    @Override
    boolean isHCondition() {
        return false;
    }

    @Override
    boolean isAction() {
        return false;
    }

    @Override
    boolean isReturn() {
        return false;
    }
}
