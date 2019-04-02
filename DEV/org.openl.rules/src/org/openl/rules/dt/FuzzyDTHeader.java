package org.openl.rules.dt;

import org.apache.commons.lang3.tuple.Triple;
import org.openl.rules.fuzzy.Token;
import org.openl.types.IOpenMethod;

class FuzzyDTHeader extends DTHeader {
    IOpenMethod[] methodsChain;
    String title;
    Triple<Token[], Integer, Integer> openlFuzzyExtractResult;
    boolean returnDTHeader;

    FuzzyDTHeader(int methodParameterIndex,
            String statement,
            String title,
            IOpenMethod[] methodsChain,
            int column,
            int width,
            Triple<Token[], Integer, Integer> openlFuzzyExtractResult,
            boolean returnDTHeader) {
        super(new int[] { methodParameterIndex }, statement, column, width);
        this.methodsChain = methodsChain;
        this.returnDTHeader = returnDTHeader;
        this.title = title;
        this.openlFuzzyExtractResult = openlFuzzyExtractResult;
    }

    public String getTitle() {
        return title;
    }

    public Triple<Token[], Integer, Integer> getOpenlFuzzyExtractResult() {
        return openlFuzzyExtractResult;
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
