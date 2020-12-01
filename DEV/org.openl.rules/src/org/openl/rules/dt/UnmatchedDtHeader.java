package org.openl.rules.dt;

class UnmatchedDtHeader extends DTHeader {
    UnmatchedDtHeader(String statement, int column, int width) {
        super(new int[] {}, statement, column, width);
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

    @Override
    boolean isRule() {
        return false;
    }
}
