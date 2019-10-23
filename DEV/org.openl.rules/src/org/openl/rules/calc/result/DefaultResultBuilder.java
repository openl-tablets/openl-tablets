package org.openl.rules.calc.result;

import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetResultCalculator;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

/**
 * Builder is used when return type of the spreadsheet table is {@link SpreadsheetResult}.
 *
 */
public class DefaultResultBuilder implements IResultBuilder {

    @Override
    public Object buildResult(SpreadsheetResultCalculator result) {

        Object[][] resultValues = result.getValues();
        final Spreadsheet spreadsheet = result.getSpreadsheet();

        SpreadsheetResult spreadsheetResult = new SpreadsheetResult(resultValues,
            spreadsheet.getRowNames(),
            spreadsheet.getColumnNames(),
            spreadsheet.getRowNamesForModel(),
            spreadsheet.getColumnNamesForModel(),
            spreadsheet.getFieldsCoordinates());

        spreadsheetResult.setDetailedPlainModel(spreadsheet.isDetailedPlainModel());

        if (spreadsheet.isCustomSpreadsheet()) {
            spreadsheetResult
                .setCustomSpreadsheetResultOpenClass((CustomSpreadsheetResultOpenClass) spreadsheet.getType());
        }

        TableSyntaxNode tsn = spreadsheet.getSyntaxNode();

        if (tsn != null) {
            spreadsheetResult.setLogicalTable(tsn.getTableBody());
        }

        return spreadsheetResult;
    }

}
