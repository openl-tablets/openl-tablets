package org.openl.rules.dt;

import java.util.Arrays;

abstract class DTHeader {
    final int[] methodParameterIndexes;
    final int column;
    final int row;
    final String statement;
    final int width;
    final int widthForMerge;
    final boolean horizontal;

    DTHeader(int[] methodParameterIndexes,
             String statement,
             int column,
             int row,
             int width,
             int widthForMerge,
             boolean horizontal) {
        this.methodParameterIndexes = methodParameterIndexes;
        this.statement = statement;
        this.column = column;
        this.row = row;
        this.width = width;
        this.widthForMerge = widthForMerge;
        this.horizontal = horizontal;
    }

    abstract boolean isCondition();

    boolean isHCondition() {
        return isCondition() && horizontal;
    }

    abstract boolean isAction();

    abstract boolean isReturn();

    abstract boolean isRule();

    int getWidth() {
        return width;
    }

    String getStatement() {
        return statement;
    }

    boolean isMethodParameterUsed() {
        return getMethodParameterIndexes().length != 0;
    }

    int[] getMethodParameterIndexes() {
        return methodParameterIndexes;
    }

    int getMethodParameterIndex() {
        if (methodParameterIndexes != null && methodParameterIndexes.length == 1) {
            return methodParameterIndexes[0];
        }
        throw new IllegalStateException();
    }

    int getColumn() {
        return column;
    }

    public int getRow() {
        return row;
    }

    public int getWidthForMerge() {
        return widthForMerge;
    }

    private String getTypeString() {
        if (isCondition()) {
            return "CONDITION";
        } else if (isAction()) {
            return "ACTION";
        } else if (isReturn()) {
            return "RETURN";
        } else if (isRule()) {
            return "RULE";
        } else {
            return "UNKNOWN";
        }
    }

    @Override
    public String toString() {
        return "DTHeader [type=" + getTypeString() + " methodParameterIndexes=" + Arrays.toString(
                methodParameterIndexes) + ", column=" + column + ", width=" + getWidth() + " statement=" + statement + "] horizontal=" + isHCondition();
    }

}
