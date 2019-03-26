package org.openl.rules.dt;

import org.openl.types.IOpenMethod;

class FuzzyDTHeader extends DTHeader {
    IOpenMethod[] methodsChain;
    boolean compoundReturn;
    String title;
    IOpenMethod[] methodChainForCompoundReturn;
    String statementForCompoundReturn;

    FuzzyDTHeader(int methodParameterIndex,
            String statement,
            String title,
            IOpenMethod[] methodsChain,
            int column,
            int width,
            String statementForCompoundReturn,
            IOpenMethod[] methodChainForCompoundReturn,
            boolean compoundReturn) {
        super(new int[] { methodParameterIndex }, statement, column, width);
        this.methodsChain = methodsChain;
        this.compoundReturn = compoundReturn;
        this.title = title;
        this.methodChainForCompoundReturn = methodChainForCompoundReturn;
        this.statementForCompoundReturn = statementForCompoundReturn;
    }

    public String getStatementForCompoundReturn() {
        return statementForCompoundReturn;
    }
    
    public IOpenMethod[] getMethodChainForCompoundReturn() {
        return methodChainForCompoundReturn;
    }

    public String getTitle() {
        return title;
    }

    @Override
    boolean isCondition() {
        return !compoundReturn;
    }

    @Override
    boolean isHCondition() {
        return false;
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
