package org.openl.studio.projects.service.tables.write;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.SpreadsheetCellView;
import org.openl.studio.projects.model.tables.SpreadsheetView;

/**
 * Writes {@link SpreadsheetView} model to legacy {@code Spreadsheet} table.
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

    public void insert(SpreadsheetCellView[][] cells) {
        try {
            table.getGridTable().edit();
            var tableBody = table.getGridTable(IXlsTableNames.VIEW_BUSINESS);
            int rowId = IGridRegion.Tool.height(tableBody.getRegion());
            for (var row : cells) {
                int colId = 1;
                for (var cell : row) {
                    createOrUpdateCell(tableBody, buildCellKey(colId, rowId), cell.value);
                    colId++;
                }
                rowId++;
            }
            save();
        } finally {
            table.getGridTable().stopEditing();
        }
    }
}
