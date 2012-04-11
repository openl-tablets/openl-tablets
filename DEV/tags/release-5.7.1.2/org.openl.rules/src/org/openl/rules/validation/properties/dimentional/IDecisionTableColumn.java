package org.openl.rules.validation.properties.dimentional;

public interface IDecisionTableColumn {
    
    String getCodeExpression();
    
    String getParameterDeclaration();
    
    String getTitle();
    
    String getRuleValue(int ruleIndex);
    
    String getRuleValue(int ruleIndex, int elementNum);
    
    boolean isArrayCondition();
    
    int getMaxNumberOfValuesForRules();
    
    String getColumnType();
    
}
