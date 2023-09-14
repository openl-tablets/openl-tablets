package org.openl.rules.rest.service.tables.write;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.rest.model.tables.SimpleRulesView;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;

/**
 * TODO description
 *
 * @author Vladyslav Pikus
 */
public class SimpleRulesWriter extends ExecutableTableWriter<SimpleRulesView> {

    private static final String RETURN_ATTR = "return";

    public SimpleRulesWriter(IOpenLTable table) {
        super(table);
    }

    @Override
    protected void updateBusinessBody(SimpleRulesView tableView) {
        var tableBody = table.getGridTable(IXlsTableNames.VIEW_BUSINESS);
        writeConditionHeaders(tableBody, tableView);
        int row = 1;
        int colMax = 0;
        for (var rule : tableView.rules) {
            int col = 0;
            for (var arg : tableView.args) {
                createOrUpdateCell(tableBody, buildCellKey(col++, row), rule.get(arg.name));
            }
            createOrUpdateCell(tableBody, buildCellKey(col++, row), rule.get(RETURN_ATTR));
            colMax = Math.max(colMax, col);
            row++;
        }

        // clean up removed columns
        var width = IGridRegion.Tool.width(tableBody.getRegion());
        if (colMax < width) {
            removeColumns(tableBody, width - colMax, colMax);
        }

        // clean up removed rows
        var height = IGridRegion.Tool.height(tableBody.getRegion());
        if (row < height) {
            removeRows(tableBody, height - row, row);
        }
    }

    private void writeConditionHeaders(IGridTable tableBody, SimpleRulesView tableView) {
        int col = 0;
        for (var arg : tableView.args) {
            createOrUpdateCell(tableBody, buildCellKey(col, 0), arg.name);
            col++;
        }
        createOrUpdateCell(tableBody, buildCellKey(col, 0), RETURN_ATTR.toUpperCase());
    }

    protected String getBusinessTableType() {
        return SimpleRulesView.TABLE_TYPE;
    }
}
