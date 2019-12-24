package org.openl.rules.dt;

import java.util.Arrays;

abstract class DTHeader {
    int[] methodParameterIndexes;
    int column;
    String statement;
    int width;

    DTHeader(int[] methodParameterIndexes, String statement, int column, int width) {
        this.methodParameterIndexes = methodParameterIndexes;
        this.statement = statement;
        this.column = column;
        this.width = width;
    }

    abstract boolean isCondition();

    abstract boolean isHCondition();

    abstract boolean isAction();

    abstract boolean isReturn();

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

    private String getTypeString() {
        if (isCondition()) {
            return "CONDITION";
        } else if (isAction()) {
            return "ACTION";
        } else if (isReturn()) {
            return "RETURN";
        } else {
            return "UNKNOWN";
        }
    }

    @Override
    public String toString() {
        return "DTHeader [type=" + getTypeString() + " methodParameterIndexes=" + Arrays.toString(
            methodParameterIndexes) + ", column=" + column + ", width=" + getWidth() + " statement=" + statement + "]";
    }

}
