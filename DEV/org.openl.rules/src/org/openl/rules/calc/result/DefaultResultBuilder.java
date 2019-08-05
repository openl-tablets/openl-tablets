package org.openl.rules.calc.result;

import java.util.Map;

import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetResultCalculator;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.Point;

/**
 * Builder is used when return type of the spreadsheet table is {@link SpreadsheetResult}.
 *
 */
public class DefaultResultBuilder implements IResultBuilder {

    @Override
    public Object makeResult(SpreadsheetResultCalculator result) {

        Object[][] resultArray = result.getValues();

        Spreadsheet spreadsheet = result.getSpreadsheet();

        String[] rowNames = spreadsheet.getRowNames();
        String[] columnNames = spreadsheet.getColumnNames();
        Map<String, Point> fieldsCoordinates = spreadsheet.getFieldsCoordinates();

        SpreadsheetResult spreadsheetBean = new SpreadsheetResult(resultArray,
            rowNames,
            columnNames,
            fieldsCoordinates);

        if (spreadsheet.getCustomSpreadsheetResultType() != null) {
            spreadsheetBean.setCustomSpreadsheetResultOpenClass(spreadsheet.getCustomSpreadsheetResultType());
        }

        TableSyntaxNode tsn = spreadsheet.getSyntaxNode();

        if (tsn != null) {
            spreadsheetBean.setLogicalTable(tsn.getTableBody());
        }

        return spreadsheetBean;
    }

}
