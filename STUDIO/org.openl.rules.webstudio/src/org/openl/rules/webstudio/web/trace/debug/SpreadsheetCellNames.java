package org.openl.rules.webstudio.web.trace.debug;

import java.util.Arrays;
import java.util.Objects;

import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetStructureBuilder;
import org.openl.rules.calc.element.SpreadsheetCell;

/**
 * Builds the OpenL display name of a spreadsheet cell, matching the legacy trace formatting.
 *
 * <p>The name is {@code $Column$Row} (for example {@code $Formula$HouseTotal}); the column part is
 * omitted when the spreadsheet has a single value column, leaving {@code $Row}.
 */
public final class SpreadsheetCellNames {

    private SpreadsheetCellNames() {
    }

    public static String of(Spreadsheet spreadsheet, SpreadsheetCell cell) {
        StringBuilder name = new StringBuilder();
        long columns = Arrays.stream(spreadsheet.getColumnNamesForResultModel()).filter(Objects::nonNull).count();
        String columnName = columns > 1 ? spreadsheet.getColumnNames()[cell.getColumnIndex()] : null;
        if (columnName != null) {
            name.append(SpreadsheetStructureBuilder.DOLLAR_SIGN).append(columnName);
        }
        name.append(SpreadsheetStructureBuilder.DOLLAR_SIGN).append(spreadsheet.getRowNames()[cell.getRowIndex()]);
        return name.toString();
    }
}
