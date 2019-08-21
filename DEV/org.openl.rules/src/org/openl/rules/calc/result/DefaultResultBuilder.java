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
    public Object makeResult(SpreadsheetResultCalculator result) {

        Object[][] resultArray = result.getValues();

        final Spreadsheet spreadsheet = result.getSpreadsheet();

        SpreadsheetResult spreadsheetBean = new SpreadsheetResult(resultArray,
            spreadsheet.getRowNames(),
            spreadsheet.getColumnNames(),
            spreadsheet.getRowNamesMarkedWithStar(),
            spreadsheet.getColumnNamesMarkedWithStar(),
            spreadsheet.getFieldsCoordinates());

        if (spreadsheet.isCustomSpreadsheetType()) {
            spreadsheetBean
                .setCustomSpreadsheetResultOpenClass((CustomSpreadsheetResultOpenClass) spreadsheet.getType());
        }

        TableSyntaxNode tsn = spreadsheet.getSyntaxNode();

        if (tsn != null) {
            spreadsheetBean.setLogicalTable(tsn.getTableBody());
        }

        return spreadsheetBean;
    }

}
