package org.openl.rules.calc.result;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetResultCalculator;

/**
 * Builder is used when return type of the spreadsheet table is {@link SpreadsheetResult}.
 */
public class SpreadsheetResultBuilder implements IResultBuilder {

    @Override
    public Object buildResult(SpreadsheetResultCalculator result) {

        var resultValues = result.getValues();
        final var spreadsheet = result.getSpreadsheet();

        var spreadsheetResult = new SpreadsheetResult(resultValues,
                spreadsheet.getRowNames(),
                spreadsheet.getColumnNames(),
                spreadsheet.getRowNamesForResultModel(),
                spreadsheet.getColumnNamesForResultModel(),
                spreadsheet.getFieldsCoordinates());

        var tsn = spreadsheet.getSyntaxNode();
        if (tsn != null) {
            spreadsheetResult.setLogicalTable(tsn.getTableBody());
        }

        // Set transient offset mappings for display purposes (physical to logical index conversion)
        spreadsheetResult.setRowOffsets(spreadsheet.getRowOffsets());
        spreadsheetResult.setColumnOffsets(spreadsheet.getColumnOffsets());

        return spreadsheetResult;
    }

}
