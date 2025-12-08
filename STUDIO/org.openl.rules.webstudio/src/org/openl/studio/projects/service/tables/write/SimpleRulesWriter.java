package org.openl.studio.projects.service.tables.write;

import java.util.List;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.studio.projects.model.tables.SimpleRulesAppend;
import org.openl.studio.projects.model.tables.SimpleRulesView;
import org.openl.studio.projects.service.tables.read.SimpleRulesTableReader;
import org.openl.util.CollectionUtils;

/**
 * Writes {@link SimpleRulesView} model to {@code SimpleRules} table.
 *
 * @author Vladyslav Pikus
 */
public class SimpleRulesWriter extends ExecutableTableWriter<SimpleRulesView> {

    public SimpleRulesWriter(IOpenLTable table) {
        super(table);
    }

    public SimpleRulesWriter(IGridTable gridTable, MetaInfoWriter metaInfoWriter) {
        super(gridTable, metaInfoWriter);
    }

    @Override
    protected void mergeHeaderCells(SimpleRulesView tableView) {
        if (!isUpdateMode()) {
            int latestCol = tableView.headers.size() - 1;
            if (CollectionUtils.isNotEmpty(tableView.properties)) {
                latestCol = NUMBER_PROPERTIES_COLUMNS - 1;
            }
            var gridTable = getGridTable();
            var mergeTitleRegion = new GridRegion(0, 0, 0, latestCol);
            applyMergeRegions(gridTable, List.of(mergeTitleRegion));
        }
    }

    @Override
    protected void updateBusinessBody(SimpleRulesView tableView) {
        var tableBody = getGridTable(IXlsTableNames.VIEW_BUSINESS);
        writeConditionHeaders(tableBody, tableView);
        int row = 1;
        int colMax = 0;
        for (var rule : tableView.rules) {
            int col = 0;
            for (var header : tableView.headers) {
                createOrUpdateCell(tableBody, buildCellKey(col++, row), rule.get(header.title));
            }
            colMax = Math.max(colMax, col);
            row++;
        }

        if (isUpdateMode()) {
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
    }

    private void writeConditionHeaders(IGridTable tableBody, SimpleRulesView tableView) {
        int col = 0;
        for (var arg : tableView.headers) {
            createOrUpdateCell(tableBody, buildCellKey(col, 0), arg.title);
            col++;
        }
    }

    public void append(SimpleRulesAppend tableAppend) {
        if (!isUpdateMode()) {
            throw new IllegalStateException("Append operation is only allowed in update mode.");
        }
        try {
            table.getGridTable().edit();
            var tableBody = table.getGridTable(IXlsTableNames.VIEW_BUSINESS);
            var headers = SimpleRulesTableReader
                    .getConditionHeaders(LogicalTableHelper.logicalTable(tableBody.getRow(0)));
            int row = IGridRegion.Tool.height(tableBody.getRegion());
            for (var rule : tableAppend.getRules()) {
                int col = 0;
                for (var header : headers) {
                    createOrUpdateCell(tableBody, buildCellKey(col++, row), rule.get(header.title));
                }
                row++;
            }
            save();
        } finally {
            table.getGridTable().stopEditing();
        }
    }

    @Override
    protected String getBusinessTableType(SimpleRulesView tableView) {
        return SimpleRulesView.TABLE_TYPE;
    }
}
