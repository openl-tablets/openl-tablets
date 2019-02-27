package org.openl.rules.dt.builder;

import org.openl.rules.table.IWritableGrid;
import org.openl.rules.validation.properties.dimentional.IDecisionTableReturnColumn;

/**
 * Builds the given return column to the given source sheet.
 * 
 * @author DLiauchuk
 *
 */
public class ReturnColumnBuilder implements IDecisionTableColumnBuilder {

    private IDecisionTableReturnColumn returnColumn;

    public ReturnColumnBuilder(IDecisionTableReturnColumn returnColumn) {
        this.returnColumn = returnColumn;
    }

    public int build(IWritableGrid sheet, int numberOfRules, int columnStartIndex, int rowStartIndex) {
        sheet.setCellValue(columnStartIndex,
            rowStartIndex + DecisionTableBuilder.COLUMN_TYPE_ROW_INDEX,
            returnColumn.getColumnType());

        sheet.setCellValue(columnStartIndex,
            rowStartIndex + DecisionTableBuilder.CODE_EXPRESSION_ROW_INDEX,
            returnColumn.getCodeExpression());

        sheet.setCellValue(columnStartIndex,
            rowStartIndex + DecisionTableBuilder.PARAMETER_DECLARATION_ROW_INDEX,
            returnColumn.getParameterDeclaration());

        sheet.setCellValue(columnStartIndex,
            rowStartIndex + DecisionTableBuilder.CONDITION_TITLE_ROW_INDEX,
            returnColumn.getTitle());

        for (int i = 0; i < numberOfRules; i++) {
            sheet.setCellValue(columnStartIndex,
                i + rowStartIndex + DecisionTableBuilder.DECISION_TABLE_HEADER_ROWS_NUMBER,
                returnColumn.getRuleValue(i));
        }
        return columnStartIndex + 1;
    }

}
