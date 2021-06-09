package org.openl.rules.dt;

import org.apache.commons.lang3.StringUtils;
import org.openl.rules.fuzzy.OpenLFuzzyUtils.FuzzyResult;

public class FuzzyRulesDTHeader extends DTHeader {
    private static final int[] EMPTY_INDEXES = new int[] {};

    private final String title;
    private final FuzzyResult fuzzyResult;

    FuzzyRulesDTHeader(String title, int column, int width, FuzzyResult fuzzyResult) {
        super(EMPTY_INDEXES, StringUtils.EMPTY, column, width);
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
        return false;
    }

    @Override
    boolean isHCondition() {
        return false;
    }

    @Override
    boolean isReturn() {
        return false;
    }

    @Override
    boolean isAction() {
        return false;
    }

    @Override
    boolean isRule() {
        return true;
    }

    boolean isMethodParameterUsed() {
        return false;
    }

    @Override
    int getMethodParameterIndex() {
        throw new IllegalStateException();
    }

    @Override
    int[] getMethodParameterIndexes() {
        return EMPTY_INDEXES;
    }
}