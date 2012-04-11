package org.openl.rules.validation.properties.dimentional;

public interface IDecisionTableColumn {
    
    /**
     * Gets the string representation of the code expression cell(the next cell after condition name definition).
     * 
     * @return string representation of the code expression cell.
     */
    String getCodeExpression();
    
    /**
     * Gets the string representation of the parameter declaration cell(the next cell after code expression cell 
     * see{@link #getCodeExpression()}).
     * 
     * @return the string representation of the parameter declaration cell
     */
    String getParameterDeclaration();
    
    /**
     * Gets the string representation of the title(business name) cell(the next cell after the parameter declaration 
     * cell see{@link #getParameterDeclaration()}).
     * 
     * @return the string representation of the title(business name) cell
     */
    String getTitle();
    
    String getRuleValue(int ruleIndex);
    
    /**
     * TODO: seems there is no sence in this method. Should be deleted    
     */
    String getRuleValue(int ruleIndex, int elementNum);
    
    boolean isArrayCondition();
    
    int getMaxNumberOfValuesForRules();
    
    /**
     * Gets the type of the column. For more information see {@link DecisionTableColumnHeaders}
     * 
     * @return string representation type of the column.
     */
    String getColumnType();
    
}
