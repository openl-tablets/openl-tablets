package org.openl.studio.projects.service.trace;

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
        // Drop the column part for a single-column spreadsheet. A short-circuiting scan (stop at the second
        // column) avoids allocating a stream per cell, which matters when profiling replays thousands of cells.
        String columnName = hasMultipleColumns(spreadsheet.getColumnNamesForResultModel())
                ? spreadsheet.getColumnNames()[cell.getColumnIndex()]
                : null;
        if (columnName != null) {
            name.append(SpreadsheetStructureBuilder.DOLLAR_SIGN).append(columnName);
        }
        name.append(SpreadsheetStructureBuilder.DOLLAR_SIGN).append(spreadsheet.getRowNames()[cell.getRowIndex()]);
        return name.toString();
    }

    private static boolean hasMultipleColumns(String[] columnNames) {
        int seen = 0;
        for (String columnName : columnNames) {
            if (columnName != null && ++seen > 1) {
                return true;
            }
        }
        return false;
    }
}
