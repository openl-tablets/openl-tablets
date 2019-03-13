package org.openl.rules.dt;

import java.util.Arrays;

abstract class DTHeader {
    int[] methodParameterIndexes;
    int column;
    String statement;

    DTHeader(int[] methodParameterIndexes, String statement, int column) {
        this.methodParameterIndexes = methodParameterIndexes;
        this.statement = statement;
        this.column = column;
    }

    abstract boolean isCondition();

    abstract boolean isAction();

    abstract boolean isReturn();

    abstract int getNumberOfUsedColumns();

    String getStatement() {
        return statement;
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
        } else {
            return "RETURN";
        }
    }

    @Override
    public String toString() {
        return "DTHeader [type=" + getTypeString() + " methodParameterIndexes=" + Arrays
            .toString(
                methodParameterIndexes) + ", column=" + column + ", numberOfUsedColumns=" + getNumberOfUsedColumns() + " statement=" + statement + "]";
    }

}
