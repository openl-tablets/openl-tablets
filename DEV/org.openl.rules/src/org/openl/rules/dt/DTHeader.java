package org.openl.rules.dt;

import java.util.Arrays;

import lombok.AccessLevel;
import lombok.Getter;

abstract class DTHeader {
    @Getter(AccessLevel.PACKAGE)
    final int[] methodParameterIndexes;
    @Getter(AccessLevel.PACKAGE)
    final int column;
    @Getter
    final int row;
    @Getter(AccessLevel.PACKAGE)
    final String statement;
    @Getter(AccessLevel.PACKAGE)
    final int width;
    @Getter
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

    boolean isMethodParameterUsed() {
        return getMethodParameterIndexes().length != 0;
    }

    int getMethodParameterIndex() {
        if (methodParameterIndexes != null && methodParameterIndexes.length == 1) {
            return methodParameterIndexes[0];
        }
        throw new IllegalStateException();
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
