package org.openl.rules.rest.service.tables.write;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.rest.model.tables.SpreadsheetView;
import org.openl.rules.table.IOpenLTable;

/**
 * TODO description
 *
 * @author Vladyslav Pikus
 */
public class SpreadsheetTableWriter extends ExecutableTableWriter<SpreadsheetView> {

    public SpreadsheetTableWriter(IOpenLTable table) {
        super(table);
    }

    @Override
    protected void updateBusinessBody(SpreadsheetView tableView) {
        var tableBody = table.getGridTable(IXlsTableNames.VIEW_BUSINESS);
        int colId = 1;
        for (var column : tableView.columns) {
            var value = SimpleSpreadsheetTableWriter.createStep(column.name, column.type);
            createOrUpdateCell(tableBody, buildCellKey(colId, 0), value);
            colId++;
        }
        int rowId = 1;
        for (var row : tableView.rows) {
            var value = SimpleSpreadsheetTableWriter.createStep(row.name, row.type);
            createOrUpdateCell(tableBody, buildCellKey(0, rowId), value);
            rowId++;
        }
        for (int row = 1; row <= tableView.rows.size(); row++) {
            for (int col = 1; col <= tableView.columns.size(); col++) {
                var cellValue = tableView.cells[row - 1][col - 1];
                createOrUpdateCell(tableBody, buildCellKey(col, row), cellValue.value);
            }
        }
    }
}
