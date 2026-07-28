package org.openl.rules.dt;

import lombok.AccessLevel;
import lombok.Getter;

import org.openl.rules.lang.xls.binding.DTColumnsDefinition;
import org.openl.types.IParameterDeclaration;

class DeclaredDTHeader extends DTHeader {
    @Getter(AccessLevel.PACKAGE)
    private final IParameterDeclaration[][] columnParameters;
    @Getter
    private final DTColumnsDefinition dtColumnsDefinition;
    @Getter(AccessLevel.PACKAGE)
    private final MatchedDefinition matchedDefinition;
    @Getter
    private final boolean verticalConditionWithMergedTitle;

    DeclaredDTHeader(int[] methodParameterIndexes,
                     DTColumnsDefinition dtColumnsDefinition,
                     IParameterDeclaration[][] columnParameters,
                     int column,
                     int row,
                     int width,
                     int widthForMerge,
                     MatchedDefinition matchedDefinition,
                     boolean horizontal,
                     boolean verticalConditionWithMergedTitle) {
        super(methodParameterIndexes, null, column, row, width, widthForMerge, horizontal);
        this.columnParameters = columnParameters;
        this.dtColumnsDefinition = dtColumnsDefinition;
        this.matchedDefinition = matchedDefinition;
        this.verticalConditionWithMergedTitle = verticalConditionWithMergedTitle;
    }

    @Override
    boolean isReturn() {
        return matchedDefinition.getDtColumnsDefinition().isReturn();
    }

    @Override
    boolean isCondition() {
        return matchedDefinition.getDtColumnsDefinition().isCondition();
    }

    @Override
    boolean isAction() {
        return matchedDefinition.getDtColumnsDefinition().isAction();
    }

    @Override
    boolean isRule() {
        return false;
    }

    @Override
    String getStatement() {
        return matchedDefinition.getStatementWithReplacedIdentifiers();
    }

}
