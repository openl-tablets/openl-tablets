package org.openl.rules.dt;

import org.openl.rules.fuzzy.OpenLFuzzyUtils.FuzzyResult;
import org.openl.types.IOpenMethod;

class FuzzyDTHeader extends DTHeader {
    IOpenMethod[] methodsChain;
    String title;
    FuzzyResult fuzzyResult;
    boolean returnDTHeader;

    FuzzyDTHeader(int methodParameterIndex,
            String statement,
            String title,
            IOpenMethod[] methodsChain,
            int column,
            int width,
            FuzzyResult fuzzyResult,
            boolean returnDTHeader) {
        super(new int[] { methodParameterIndex }, statement, column, width);
        this.methodsChain = methodsChain;
        this.returnDTHeader = returnDTHeader;
        this.title = title;
        this.fuzzyResult = fuzzyResult;
    }

    public String getTitle() {
        return title;
    }

    public FuzzyResult getFuzzyResult() {
        return fuzzyResult;
    }

    @Override
    boolean isCondition() {
        return !returnDTHeader;
    }

    @Override
    boolean isHCondition() {
        return false;
    }

    @Override
    boolean isReturn() {
        return returnDTHeader;
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
        if (returnDTHeader) {
            throw new IllegalStateException();
        }
        return super.getMethodParameterIndex();
    }

    private static final int[] RETURN_INDEXES = new int[] {};

    @Override
    int[] getMethodParameterIndexes() {
        if (returnDTHeader) {
            return RETURN_INDEXES;
        }
        return super.getMethodParameterIndexes();
    }
}
