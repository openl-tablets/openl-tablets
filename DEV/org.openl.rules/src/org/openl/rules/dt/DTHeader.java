package org.openl.rules.dt;

import java.util.Arrays;

abstract class DTHeader {
    final int[] methodParameterIndexes;
    final int column;
    final int row;
    final String statement;
    final int width;

    DTHeader(int[] methodParameterIndexes, String statement, int column, int row, int width) {
        this.methodParameterIndexes = methodParameterIndexes;
        this.statement = statement;
        this.column = column;
        this.row = row;
        this.width = width;
    }

    abstract boolean isCondition();

    abstract boolean isHCondition();

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
            methodParameterIndexes) + ", column=" + column + ", width=" + getWidth() + " statement=" + statement + "]";
    }

}
