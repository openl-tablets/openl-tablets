package org.openl.rules.dt;

import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.CompositeMethod;

class DeclaredDTHeader extends DTHeader {
    private final IParameterDeclaration[][] columnParameters;
    private final CompositeMethod compositeMethod;
    private final MatchedDefinition matchedDefinition;

    DeclaredDTHeader(int[] methodParameterIndexes,
            CompositeMethod compositeMethod,
            IParameterDeclaration[][] columnParameters,
            int column,
            int row,
            int width,
            int widthForMerge,
            MatchedDefinition matchedDefinition,
            boolean horizontal) {
        super(methodParameterIndexes, null, column, row, width, widthForMerge, horizontal);
        this.columnParameters = columnParameters;
        this.compositeMethod = compositeMethod;
        this.matchedDefinition = matchedDefinition;
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

    CompositeMethod getCompositeMethod() {
        return compositeMethod;
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
