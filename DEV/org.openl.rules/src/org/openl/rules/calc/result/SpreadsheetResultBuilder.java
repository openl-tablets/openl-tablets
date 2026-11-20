package org.openl.rules.calc.result;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetResultCalculator;

/**
 * Builder is used when return type of the spreadsheet table is {@link SpreadsheetResult}.
 */
public class SpreadsheetResultBuilder implements IResultBuilder {

    @Override
    public Object buildResult(SpreadsheetResultCalculator result) {
        var spreadsheet = result.getSpreadsheet();
        return new SpreadsheetResult(result.getValues(),
                spreadsheet.getRowNames(),
                spreadsheet.getColumnNames(),
                spreadsheet.getRowNamesForResultModel(),
                spreadsheet.getColumnNamesForResultModel(),
                spreadsheet.getFieldsCoordinates());
    }

}
