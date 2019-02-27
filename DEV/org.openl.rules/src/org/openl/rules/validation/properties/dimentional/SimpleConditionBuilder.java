package org.openl.rules.validation.properties.dimentional;

import org.openl.rules.table.IWritableGrid;

public class SimpleConditionBuilder extends AConditionBuilder {

    /**
     * 
     * @param condition that should be written to the source sheet
     * @param conditionNumber number of the given condition in the Decision Table
     */
    SimpleConditionBuilder(IDecisionTableColumn condition, int conditionNumber) {
        super(condition, conditionNumber);
    }

    @Override
    protected void writeColumnType(IWritableGrid sheet, int columnStartIndex, int rowStartIndex) {
        sheet.setCellValue(columnStartIndex,
            rowStartIndex + DecisionTableBuilder.COLUMN_TYPE_ROW_INDEX,
            getCondition().getColumnType() + getConditionNumber());
    }

    @Override
    protected void writeCodeExpression(IWritableGrid sheet, int columnStartIndex, int rowStartIndex) {
        sheet.setCellValue(columnStartIndex,
            rowStartIndex + DecisionTableBuilder.CODE_EXPRESSION_ROW_INDEX,
            getCondition().getCodeExpression());
    }

    @Override
    protected void writeParameterDeclaration(IWritableGrid sheet, int columnStartIndex, int rowStartIndex) {
        sheet.setCellValue(columnStartIndex,
            rowStartIndex + DecisionTableBuilder.PARAMETER_DECLARATION_ROW_INDEX,
            getCondition().getParameterDeclaration());
    }

    @Override
    protected void writeTitle(IWritableGrid sheet, int columnStartIndex, int rowStartIndex) {
        sheet.setCellValue(columnStartIndex,
            rowStartIndex + DecisionTableBuilder.CONDITION_TITLE_ROW_INDEX,
            getCondition().getTitle());
    }

    @Override
    protected void writeRuleValue(IWritableGrid sheet, int numberOfRules, int columnStartIndex, int rowStartIndex) {
        for (int i = 0; i < numberOfRules; i++) {
            sheet.setCellValue(columnStartIndex,
                rowStartIndex + DecisionTableBuilder.DECISION_TABLE_HEADER_ROWS_NUMBER + i,
                getCondition().getRuleValue(i));
        }
    }

}
