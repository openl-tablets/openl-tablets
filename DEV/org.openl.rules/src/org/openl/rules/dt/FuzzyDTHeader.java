package org.openl.rules.dt;

import org.openl.rules.fuzzy.OpenLFuzzyUtils.FuzzyResult;
import org.openl.types.IOpenMethod;

class FuzzyDTHeader extends DTHeader {
    private static final int[] RETURN_EMPTY_INDEXES = new int[] {};

    private IOpenMethod[] methodsChain;
    private String title;
    private FuzzyResult fuzzyResult;
    private int topColumn;
    private boolean returnDTHeader;

    FuzzyDTHeader(int methodParameterIndex,
            String statement,
            String title,
            IOpenMethod[] methodsChain,
            int topColumn,
            int column,
            int width,
            FuzzyResult fuzzyResult,
            boolean returnDTHeader) {
        super(new int[] { methodParameterIndex }, statement, column, width);
        this.topColumn = topColumn;
        this.methodsChain = methodsChain;
        this.returnDTHeader = returnDTHeader;
        this.title = title;
        this.fuzzyResult = fuzzyResult;
    }

    String getTitle() {
        return title;
    }

    FuzzyResult getFuzzyResult() {
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

    @Override
    int[] getMethodParameterIndexes() {
        if (returnDTHeader) {
            return RETURN_EMPTY_INDEXES;
        }
        return super.getMethodParameterIndexes();
    }

    public int getTopColumn() {
        return topColumn;
    }
}
