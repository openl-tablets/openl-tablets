package org.openl.rules.dt;

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

}
