package org.openl.rules.dt;

class UnmatchedDtHeader extends DTHeader {
    private final boolean vertical;

    UnmatchedDtHeader(String statement, int column, int row, int width, boolean vertical) {
        super(new int[] {}, statement, column, row, width, width);
        this.vertical = vertical;
    }

    @Override
    boolean isCondition() {
        return vertical;
    }

    @Override
    boolean isHCondition() {
        return vertical;
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
