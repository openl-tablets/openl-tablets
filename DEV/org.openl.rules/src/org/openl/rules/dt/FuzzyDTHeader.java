package org.openl.rules.dt;

import org.openl.types.IOpenMethod;

class FuzzyDTHeader extends DTHeader {
    IOpenMethod[] methodsChain;
    boolean compoundReturn;
    String title;

    FuzzyDTHeader(int methodParameterIndex,
            String statement,
            String title,
            IOpenMethod[] methodsChain,
            int column,
            boolean compoundReturn) {
        super(new int[] { methodParameterIndex }, statement, column);
        this.methodsChain = methodsChain;
        this.compoundReturn = compoundReturn;
        this.title = title;
    }
    
    public String getTitle() {
        return title;
    }

    @Override
    int getNumberOfUsedColumns() {
        return 1;
    }

    @Override
    boolean isCondition() {
        return !compoundReturn;
    }

    @Override
    boolean isReturn() {
        return compoundReturn;
    }

    IOpenMethod[] getMethodsChain() {
        return methodsChain;
    }
    
    @Override
    boolean isAction() {
        return false;
    }
    
    @Override
    int getMethodParameterIndex() {
        if (compoundReturn) {
            throw new IllegalStateException();
        }
        return super.getMethodParameterIndex();
    }
    
    private final static int[] RETURN_INDEXES = new int[] {};
    
    @Override
    int[] getMethodParameterIndexes() {
        if (compoundReturn) {
            return RETURN_INDEXES;
        }
        return super.getMethodParameterIndexes();
    }
}
