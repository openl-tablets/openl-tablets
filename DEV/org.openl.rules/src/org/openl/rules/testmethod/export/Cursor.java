package org.openl.rules.testmethod.export;

final class Cursor {
    private final int rowNum;
    private final int colNum;

    Cursor(int rowNum, int colNum) {
        this.rowNum = rowNum;
        this.colNum = colNum;
    }

    public int getRowNum() {
        return rowNum;
    }

    public int getColNum() {
        return colNum;
    }
}
