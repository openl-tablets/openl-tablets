package org.openl.rules.dt;

import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.CompositeMethod;

class DeclaredDTHeader extends DTHeader {
    IParameterDeclaration[][] columnParameters;
    CompositeMethod compositeMethod;
    MatchedDefinition matchedDefinition;
    
    public DeclaredDTHeader(int[] methodParameterIndexes,
            CompositeMethod compositeMethod,
            IParameterDeclaration[][] columnParameters,
            int column,
            MatchedDefinition matchedDefinition) {
        super(methodParameterIndexes, null, column);
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
    
    CompositeMethod getCompositeMethod() {
        return compositeMethod;
    }
    
    @Override
    String getStatement() {
        return matchedDefinition.getStatement();
    }
    
    IParameterDeclaration[][] getColumnParameters() {
        return columnParameters;
    }
    
    public MatchedDefinition getMatchedDefinition() {
        return matchedDefinition;
    }
    
    @Override
    int getNumberOfUsedColumns() {
        return matchedDefinition.getDtColumnsDefinition().getNumberOfTitles();
    }
}
