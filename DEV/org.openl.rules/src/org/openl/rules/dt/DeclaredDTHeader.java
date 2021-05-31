package org.openl.rules.dt;

import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.CompositeMethod;

class DeclaredDTHeader extends DTHeader {
    private final IParameterDeclaration[][] columnParameters;
    private final CompositeMethod compositeMethod;
    private final MatchedDefinition matchedDefinition;
    private final boolean vertical;

    DeclaredDTHeader(int[] methodParameterIndexes,
            CompositeMethod compositeMethod,
            IParameterDeclaration[][] columnParameters,
            int column,
            int row,
            int width,
            MatchedDefinition matchedDefinition,
            boolean vertical) {
        super(methodParameterIndexes, null, column, row, width);
        this.columnParameters = columnParameters;
        this.compositeMethod = compositeMethod;
        this.matchedDefinition = matchedDefinition;
        this.vertical = vertical;
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
    boolean isHCondition() {
        return vertical;
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
