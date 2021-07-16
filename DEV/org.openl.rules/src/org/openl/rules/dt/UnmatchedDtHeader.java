package org.openl.rules.dt;

class UnmatchedDtHeader extends DTHeader {
    UnmatchedDtHeader(String statement, int column, int row, int width, boolean horizontal) {
        super(new int[] {}, statement, column, row, width, width, horizontal);
    }

    @Override
    boolean isCondition() {
        return true;
    }

    @Override
    boolean isHCondition() {
        return horizontal;
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
