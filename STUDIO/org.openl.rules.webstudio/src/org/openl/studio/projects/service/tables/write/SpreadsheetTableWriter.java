package org.openl.studio.projects.service.tables.write;

import java.util.List;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.SpreadsheetCellView;
import org.openl.studio.projects.model.tables.SpreadsheetView;
import org.openl.util.CollectionUtils;

/**
 * Writes {@link SpreadsheetView} model to legacy {@code Spreadsheet} table.
 *
 * @author Vladyslav Pikus
 */
public class SpreadsheetTableWriter extends ExecutableTableWriter<SpreadsheetView> {

    private static final int ROW_HEADER_COL_IDX = 0;
    private static final int FIRST_DATA_COL_IDX = 1;

    public SpreadsheetTableWriter(IOpenLTable table) {
        super(table);
    }

    public SpreadsheetTableWriter(IGridTable gridTable, MetaInfoWriter metaInfoWriter) {
        super(gridTable, metaInfoWriter);
    }

    @Override
    protected void mergeHeaderCells(SpreadsheetView tableView) {
        if (!isUpdateMode()) {
            int latestCol = tableView.columns.size();
            if (CollectionUtils.isNotEmpty(tableView.properties)) {
                latestCol = Math.max(NUMBER_PROPERTIES_COLUMNS - 1, latestCol);
            }
            var mergeTitleRegion = new GridRegion(0, 0, 0, latestCol);
            applyMergeRegions(getGridTable(), List.of(mergeTitleRegion));
        }
    }


    /**
     * Writes business body of Spreadsheet table, which has the following structure:
     * <table>
     *   <tr>
     *     <td></td> <td> Col Header 1</td> <td> Col Header 2</td>
     *   </tr>
     *   <tr>
     *     <td> Row Header 1 </td> <td> value 11</td><td> value 12</td>
     *   </tr>
     *   <tr>
     *     <td> Row Header 2 </td> <td> value 21</td><td> value 22</td>
     *   </tr>
     * </table>
     *
     * @param tableView the spreadsheet view model
     */
    @Override
    protected void updateBusinessBody(SpreadsheetView tableView) {
        var tableBody = getGridTable(IXlsTableNames.VIEW_BUSINESS);

        final int headerRowIndex = 0;
        final int firstStepRowIndex = headerRowIndex + 1;

        // Step 1. Write column names and types
        for (int colNum = 0; colNum < tableView.columns.size(); colNum++) {
            var column = tableView.columns.get(colNum);
            var value = SimpleSpreadsheetTableWriter.createStep(column.name, column.type);
            createOrUpdateCell(tableBody, buildCellKey(colNum + FIRST_DATA_COL_IDX, headerRowIndex), value);
        }
        // Step 2. Write row names and types
        for (int rowNum = 0; rowNum < tableView.rows.size(); rowNum++) {
            var row = tableView.rows.get(rowNum);
            var value = SimpleSpreadsheetTableWriter.createStep(row.name, row.type);
            createOrUpdateCell(tableBody, buildCellKey(ROW_HEADER_COL_IDX, rowNum + firstStepRowIndex), value);
        }
        // Step 3. Write cell values
        for (int row = 0; row < tableView.rows.size(); row++) {
            for (int col = 0; col < tableView.columns.size(); col++) {
                var cellValue = tableView.cells[row][col];
                createOrUpdateCell(tableBody, buildCellKey(col + FIRST_DATA_COL_IDX, row + firstStepRowIndex), cellValue.value);
            }
        }
    }

    public void insert(SpreadsheetCellView[][] cells) {
        if (!isUpdateMode()) {
            throw new IllegalStateException("Append operation is only allowed in update mode.");
        }
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
