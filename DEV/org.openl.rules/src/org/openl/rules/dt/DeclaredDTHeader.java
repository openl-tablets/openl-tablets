package org.openl.rules.dt;

import org.openl.rules.lang.xls.binding.DTColumnsDefinition;
import org.openl.types.IParameterDeclaration;

class DeclaredDTHeader extends DTHeader {
    private final IParameterDeclaration[][] columnParameters;
    private final DTColumnsDefinition dtColumnsDefinition;
    private final MatchedDefinition matchedDefinition;
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

    public boolean isVerticalConditionWithMergedTitle() {
        return verticalConditionWithMergedTitle;
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

    public DTColumnsDefinition getDtColumnsDefinition() {
        return dtColumnsDefinition;
    }

    @Override
    String getStatement() {
        return matchedDefinition.getStatementWithReplacedIdentifiers();
    }

    IParameterDeclaration[][] getColumnParameters() {
        return columnParameters;
    }

    MatchedDefinition getMatchedDefinition() {
        return matchedDefinition;
    }

}
