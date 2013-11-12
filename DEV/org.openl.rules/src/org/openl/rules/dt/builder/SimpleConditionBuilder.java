package org.openl.rules.dt.builder;

import org.openl.rules.table.IWritableGrid;
import org.openl.rules.validation.properties.dimentional.IDecisionTableColumn;

public class SimpleConditionBuilder extends AConditionBuilder {
    
    /**
     * 
     * @param condition that should be written to the source sheet
     * @param conditionNumber number of the given condition in the Decision Table
     */
    public SimpleConditionBuilder(IDecisionTableColumn condition, int conditionNumber) {        
        super(condition, conditionNumber);
    }
    
    public void writeColumnType(IWritableGrid sheet, int columnStartIndex, int rowStartIndex) {
        sheet.setCellValue(columnStartIndex, rowStartIndex + DecisionTableBuilder.COLUMN_TYPE_ROW_INDEX, 
            String.format("%s%s", getCondition().getColumnType(), getConditionNumber()));        
    }
    
    public void writeCodeExpression(IWritableGrid sheet, int columnStartIndex, int rowStartIndex) {
        sheet.setCellValue(columnStartIndex, rowStartIndex + DecisionTableBuilder.CODE_EXPRESSION_ROW_INDEX, 
            getCondition().getCodeExpression());
    }
    
    public void writeParameterDeclaration(IWritableGrid sheet, int columnStartIndex, int rowStartIndex) {
        sheet.setCellValue(columnStartIndex, rowStartIndex + DecisionTableBuilder.PARAMETER_DECLARATION_ROW_INDEX, 
            getCondition().getParameterDeclaration());
    }
    
    public void writeTitle(IWritableGrid sheet, int columnStartIndex, int rowStartIndex) {
        sheet.setCellValue(columnStartIndex, rowStartIndex + DecisionTableBuilder.CONDITION_TITLE_ROW_INDEX, 
            getCondition().getTitle());
    }
    

    public void writeRuleValue(IWritableGrid sheet, int numberOfRules, int columnStartIndex, int rowStartIndex) {
        for (int i = 0; i < numberOfRules; i++) {
            sheet.setCellValue(columnStartIndex, rowStartIndex + 
                DecisionTableBuilder.DECISION_TABLE_HEADER_ROWS_NUMBER + i, 
                getCondition().getRuleValue(i));            
        }
    }    

}
