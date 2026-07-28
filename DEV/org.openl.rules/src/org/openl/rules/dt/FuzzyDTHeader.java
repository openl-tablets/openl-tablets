package org.openl.rules.dt;

import lombok.AccessLevel;
import lombok.Getter;

import org.openl.rules.fuzzy.OpenLFuzzyUtils.FuzzyResult;
import org.openl.types.IOpenField;

class FuzzyDTHeader extends DTHeader {
    private static final int[] EMPTY_INDEXES = new int[]{};

    @Getter(AccessLevel.PACKAGE)
    private final IOpenField[] fieldsChain;
    @Getter(AccessLevel.PACKAGE)
    private final String title;
    @Getter(AccessLevel.PACKAGE)
    private final FuzzyResult fuzzyResult;
    @Getter
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
        super(new int[]{methodParameterIndex}, statement, column, row, width, widthForMerge, horizontal);
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

    @Override
    boolean isCondition() {
        return !returnDTHeader;
    }

    @Override
    boolean isReturn() {
        return returnDTHeader;
    }

    @Override
    boolean isAction() {
        return false;
    }

    @Override
    boolean isRule() {
        return false;
    }

    @Override
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
}
