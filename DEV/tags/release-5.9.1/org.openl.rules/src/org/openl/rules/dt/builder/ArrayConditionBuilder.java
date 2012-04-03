package org.openl.rules.dt.builder;

import org.openl.rules.table.GridRegion;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.validation.properties.dimentional.IDecisionTableColumn;

public class ArrayConditionBuilder extends AConditionBuilder {
    
    public ArrayConditionBuilder(IDecisionTableColumn condition, int conditionNumber) {        
        super(condition, conditionNumber);
    }

    public void writeColumnType(IWritableGrid sheet, int columnStartIndex, int rowStartIndex) {
        sheet.setCellValue(columnStartIndex, rowStartIndex + DecisionTableBuilder.COLUMN_TYPE_ROW_INDEX, 
            String.format("%s%s",  getCondition().getColumnType(), getConditionNumber()));
        
        mergeArrayCells(sheet, DecisionTableBuilder.COLUMN_TYPE_ROW_INDEX, columnStartIndex, 
            getCondition().getNumberOfLocalParameters());
        
    }
    
    public void writeCodeExpression(IWritableGrid sheet, int columnStartIndex, int rowStartIndex) {        
        sheet.setCellValue(columnStartIndex, rowStartIndex + DecisionTableBuilder.CODE_EXPRESSION_ROW_INDEX, 
            getCondition().getCodeExpression());
        
        mergeArrayCells(sheet, DecisionTableBuilder.CODE_EXPRESSION_ROW_INDEX, columnStartIndex, 
            getCondition().getNumberOfLocalParameters()); 
        
    }    

    public void writeParameterDeclaration(IWritableGrid sheet, int columnStartIndex, int rowStartIndex) {
        for (int i = 0; i < getCondition().getNumberOfLocalParameters(); i ++) {
            sheet.setCellValue(columnStartIndex, rowStartIndex + DecisionTableBuilder.PARAMETER_DECLARATION_ROW_INDEX, 
                String.format("%s%d", getCondition().getParameterDeclaration(), (i + 1)));
            
            columnStartIndex++;
        }        
    }    

    public void writeTitle(IWritableGrid sheet, int columnStartIndex, int rowStartIndex) {
        sheet.setCellValue(columnStartIndex, rowStartIndex + DecisionTableBuilder.CONDITION_TITLE_ROW_INDEX, 
            getCondition().getTitle());
        
        mergeArrayCells(sheet, DecisionTableBuilder.CONDITION_TITLE_ROW_INDEX, columnStartIndex, 
            getCondition().getNumberOfLocalParameters());
        
    }
    
    public void writeRuleValue(IWritableGrid sheet, int numberOfRules, int columnStartIndex, int rowStartIndex) {
        int startCol = columnStartIndex;
        for (int i = 0; i < numberOfRules; i++) {
            for (int j = 0; j < getCondition().getNumberOfLocalParameters(); j++) {
                sheet.setCellValue(columnStartIndex, i + rowStartIndex + 
                    DecisionTableBuilder.DECISION_TABLE_HEADER_ROWS_NUMBER, 
                    getCondition().getRuleValue(i, columnStartIndex - startCol));
                
                columnStartIndex++;
            }
            columnStartIndex = startCol;
        }        
    }
    
    private void mergeArrayCells(IWritableGrid sheet, int rowIndex, int columnIndex, int numberOfValues) {
        // counting begins from 0
        int lastMergedColumnIndex = columnIndex + numberOfValues - 1;
        sheet.addMergedRegion(new GridRegion(rowIndex, columnIndex, rowIndex, lastMergedColumnIndex));
    }

}
