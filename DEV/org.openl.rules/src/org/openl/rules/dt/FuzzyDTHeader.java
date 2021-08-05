package org.openl.rules.dt;

import org.openl.rules.fuzzy.OpenLFuzzyUtils.FuzzyResult;
import org.openl.types.IOpenField;

class FuzzyDTHeader extends DTHeader {
    private static final int[] EMPTY_INDEXES = new int[] {};

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
            int row,
            int width,
            int widthForMerge,
            FuzzyResult fuzzyResult,
            boolean returnDTHeader,
            boolean horizontal) {
        super(new int[] { methodParameterIndex }, statement, column, row, width, widthForMerge, horizontal);
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
            int row,
            int width,
            int widthForMerge,
            FuzzyResult fuzzyResult,
            boolean returnDTHeader,
            boolean horizontal) {
        super(EMPTY_INDEXES, statement, column, row, width, widthForMerge, horizontal);
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

    @Override
    boolean isRule() {
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
            return EMPTY_INDEXES;
        }
        return super.getMethodParameterIndexes();
    }

    public int getTopColumn() {
        return topColumn;
    }
}
