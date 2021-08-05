package org.openl.rules.dt;

class SimpleDTHeader extends DTHeader {

    private String title;

    SimpleDTHeader(int methodParameterIndex, String statement, String title, int column, int row, int width) {
        super(new int[] { methodParameterIndex }, statement, column, row, width, width, false);
        this.title = title;
    }

    SimpleDTHeader(int methodParameterIndex, String statement, int column, int row) {
        super(new int[] { methodParameterIndex }, statement, column, row, 1, 1, true);
    }

    public String getTitle() {
        return title;
    }

    public int getRow() {
        return row;
    }

    @Override
    boolean isCondition() {
        return true;
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
