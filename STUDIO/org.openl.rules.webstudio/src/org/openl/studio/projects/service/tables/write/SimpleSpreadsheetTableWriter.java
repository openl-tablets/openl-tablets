package org.openl.studio.projects.service.tables.write;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.SimpleSpreadsheetAppend;
import org.openl.studio.projects.model.tables.SimpleSpreadsheetView;
import org.openl.studio.projects.model.tables.SpreadsheetStepView;
import org.openl.util.StringUtils;

/**
 * Writes {@link SimpleSpreadsheetView} model to simple {@code Spreadsheet} table.
 *
 * @author Vladyslav Pikus
 */
public class SimpleSpreadsheetTableWriter extends ExecutableTableWriter<SimpleSpreadsheetView> {

    private static final char TYPE_DELIM = ':';

    private static final int STEP_NAME_COLUMN = 0;
    private static final int STEP_VALUE_COLUMN = 1;

    public SimpleSpreadsheetTableWriter(IOpenLTable table) {
        super(table);
    }

    @Override
    protected void mergeHeaderCells(SimpleSpreadsheetView tableView) {

    }

    @Override
    protected void updateBusinessBody(SimpleSpreadsheetView tableView) {
        var tableBody = table.getGridTable(IXlsTableNames.VIEW_BUSINESS);

        int row = 0;
        createOrUpdateCell(tableBody, buildCellKey(STEP_NAME_COLUMN, row), "Steps");
        createOrUpdateCell(tableBody, buildCellKey(STEP_VALUE_COLUMN, row), "Value");
        row++;
        for (var step : tableView.steps) {
            write(tableBody, row, step);
            row++;
        }

        // clean up removed columns
        var width = IGridRegion.Tool.width(tableBody.getRegion());
        if (2 < width) {
            removeColumns(tableBody, width - 2, 2);
        }

        // clean up removed rows
        var height = IGridRegion.Tool.height(tableBody.getRegion());
        if (row < height) {
            removeRows(tableBody, height - row, row);
        }
    }

    private void write(IGridTable tableBody, int row, SpreadsheetStepView stepView) {
        createOrUpdateCell(tableBody, buildCellKey(STEP_NAME_COLUMN, row), createStep(stepView.name, stepView.type));
        createOrUpdateCell(tableBody, buildCellKey(STEP_VALUE_COLUMN, row), stepView.value);
    }

    public static String createStep(String name, String type) {
        var stepDeclaration = name;
        if (StringUtils.isNotBlank(type)) {
            stepDeclaration += TYPE_DELIM + type;
        }
        return stepDeclaration;
    }

    public void append(SimpleSpreadsheetAppend appendTable) {
        try {
            table.getGridTable().edit();
            var tableBody = table.getGridTable(IXlsTableNames.VIEW_BUSINESS);
            int row = IGridRegion.Tool.height(tableBody.getRegion());
            for (var step : appendTable.getSteps()) {
                write(tableBody, row, step);
                row++;
            }
            save();
        } finally {
            table.getGridTable().stopEditing();
        }
    }
}
