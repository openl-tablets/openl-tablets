package org.openl.rules.validation.properties.dimentional;

import org.openl.rules.table.GridRegion;
import org.openl.rules.table.IWritableGrid;

public class ArrayConditionBuilder extends AConditionBuilder {

    ArrayConditionBuilder(IDecisionTableColumn condition, int conditionNumber) {
        super(condition, conditionNumber);
    }

    @Override
    protected void writeColumnType(IWritableGrid sheet, int columnStartIndex, int rowStartIndex) {
        sheet.setCellValue(columnStartIndex,
                rowStartIndex + DecisionTableBuilder.COLUMN_TYPE_ROW_INDEX,
                getCondition().getColumnType() + getConditionNumber());

        mergeArrayCells(sheet,
                DecisionTableBuilder.COLUMN_TYPE_ROW_INDEX,
                columnStartIndex,
                getCondition().getNumberOfLocalParameters());
    }

    @Override
    protected void writeCodeExpression(IWritableGrid sheet, int columnStartIndex, int rowStartIndex) {
        sheet.setCellValue(columnStartIndex,
                rowStartIndex + DecisionTableBuilder.CODE_EXPRESSION_ROW_INDEX,
                getCondition().getCodeExpression());

        mergeArrayCells(sheet,
                DecisionTableBuilder.CODE_EXPRESSION_ROW_INDEX,
                columnStartIndex,
                getCondition().getNumberOfLocalParameters());
    }

    @Override
    protected void writeParameterDeclaration(IWritableGrid sheet, int columnStartIndex, int rowStartIndex) {
        final var condition = getCondition();
        final var numberOfLocalParameters = condition.getNumberOfLocalParameters();
        final var parameterDeclaration = condition.getParameterDeclaration();
        for (var i = 1; i <= numberOfLocalParameters; i++) {
            sheet.setCellValue(columnStartIndex,
                    rowStartIndex + DecisionTableBuilder.PARAMETER_DECLARATION_ROW_INDEX,
                    parameterDeclaration + i);

            columnStartIndex++;
        }
    }

    @Override
    protected void writeTitle(IWritableGrid sheet, int columnStartIndex, int rowStartIndex) {
        sheet.setCellValue(columnStartIndex,
                rowStartIndex + DecisionTableBuilder.CONDITION_TITLE_ROW_INDEX,
                getCondition().getTitle());

        mergeArrayCells(sheet,
                DecisionTableBuilder.CONDITION_TITLE_ROW_INDEX,
                columnStartIndex,
                getCondition().getNumberOfLocalParameters());
    }

    @Override
    protected void writeRuleValue(IWritableGrid sheet, int numberOfRules, int columnStartIndex, int rowStartIndex) {
        var startCol = columnStartIndex;
        for (var i = 0; i < numberOfRules; i++) {
            for (var j = 0; j < getCondition().getNumberOfLocalParameters(); j++) {
                sheet.setCellValue(columnStartIndex,
                        i + rowStartIndex + DecisionTableBuilder.DECISION_TABLE_HEADER_ROWS_NUMBER,
                        getCondition().getRuleValue(i, columnStartIndex - startCol));

                columnStartIndex++;
            }
            columnStartIndex = startCol;
        }
    }

    private static void mergeArrayCells(IWritableGrid sheet, int rowIndex, int columnIndex, int numberOfValues) {
        // counting begins from 0
        var lastMergedColumnIndex = columnIndex + numberOfValues - 1;
        sheet.addMergedRegion(new GridRegion(rowIndex, columnIndex, rowIndex, lastMergedColumnIndex));
    }

}
