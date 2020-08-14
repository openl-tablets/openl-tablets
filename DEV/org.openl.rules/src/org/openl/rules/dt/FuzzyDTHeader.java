package org.openl.rules.dt;

import org.openl.rules.fuzzy.OpenLFuzzyUtils.FuzzyResult;
import org.openl.types.IOpenField;

class FuzzyDTHeader extends DTHeader {
    private static final int[] RETURN_EMPTY_INDEXES = new int[] {};

    private final IOpenField[] fieldsChain;
    private final String title;
    private final FuzzyResult fuzzyResult;
    private final int topColumn;
    private final boolean returnDTHeader;

    FuzzyDTHeader(int methodParameterIndex,
            String statement,
            String title,
            IOpenField[] fieldsChain,
            int topColumn,
            int column,
            int width,
            FuzzyResult fuzzyResult,
            boolean returnDTHeader) {
        super(new int[] { methodParameterIndex }, statement, column, width);
        this.topColumn = topColumn;
        this.fieldsChain = fieldsChain;
        this.returnDTHeader = returnDTHeader;
        this.title = title;
        this.fuzzyResult = fuzzyResult;
    }

    FuzzyDTHeader(String statement,
            String title,
            IOpenField[] fieldsChain,
            int topColumn,
            int column,
            int width,
            FuzzyResult fuzzyResult,
            boolean returnDTHeader) {
        super(new int[] {}, statement, column, width);
        this.topColumn = topColumn;
        this.fieldsChain = fieldsChain;
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

    IOpenField[] getFieldsChain() {
        return fieldsChain;
    }

    @Override
    boolean isAction() {
        return false;
    }

    boolean isMethodParameterUsed() {
        if (returnDTHeader) {
            return false;
        }
        return super.isMethodParameterUsed();
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
