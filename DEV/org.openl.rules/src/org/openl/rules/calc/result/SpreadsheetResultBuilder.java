package org.openl.rules.calc.result;

import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetResultCalculator;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

/**
 * Builder is used when return type of the spreadsheet table is {@link SpreadsheetResult}.
 */
public class SpreadsheetResultBuilder implements IResultBuilder {

    @Override
    public Object buildResult(SpreadsheetResultCalculator result) {

        Object[][] resultValues = result.getValues();
        final Spreadsheet spreadsheet = result.getSpreadsheet();

        SpreadsheetResult spreadsheetResult = new SpreadsheetResult(resultValues,
                spreadsheet.getRowNames(),
                spreadsheet.getColumnNames(),
                spreadsheet.getRowNamesForResultModel(),
                spreadsheet.getColumnNamesForResultModel(),
                spreadsheet.getFieldsCoordinates());

        TableSyntaxNode tsn = spreadsheet.getSyntaxNode();
        if (tsn != null) {
            spreadsheetResult.setLogicalTable(tsn.getTableBody());
        }

        // Set transient offset mappings for display purposes (physical to logical index conversion)
        spreadsheetResult.setRowOffsets(spreadsheet.getRowOffsets());
        spreadsheetResult.setColumnOffsets(spreadsheet.getColumnOffsets());

        return spreadsheetResult;
    }

}
