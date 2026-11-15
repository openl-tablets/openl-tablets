package org.openl.studio.projects.service.tables.write;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

import org.jspecify.annotations.Nullable;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.GridRegionUtils;
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
            var latestCol = tableView.headers.size() - 1;
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
        if (isUpdateMode()) {
            unmergeSplitValues(tableBody, tableView);
        }
        var row = 1;
        var colMax = 0;
        for (var rule : tableView.rules) {
            var col = 0;
            for (var header : tableView.headers) {
                createOrUpdateCell(tableBody, buildCellKey(col++, row), rule.get(header.title));
            }
            colMax = Math.max(colMax, col);
            row++;
        }

        if (isUpdateMode()) {
            // clean up removed columns
            var width = GridRegionUtils.width(tableBody.getRegion());
            if (colMax < width) {
                removeColumns(tableBody, width - colMax, colMax);
            }

            // clean up removed rows
            var height = GridRegionUtils.height(tableBody.getRegion());
            if (row < height) {
                removeRows(tableBody, height - row, row);
            }
        }
    }

    /**
     * Unmerges a value merged over several rules when the written rules give them different values, so that each rule
     * keeps its own. A value merged over rules that keep one value stays merged.
     */
    private void unmergeSplitValues(IGridTable tableBody, SimpleRulesView tableView) {
        var grid = tableBody.getGrid();
        var table = tableBody.getRegion();
        var height = GridRegionUtils.height(table);
        for (var col = 0; col < tableView.headers.size(); col++) {
            var title = tableView.headers.get(col).title;
            var row = 1;
            while (row < height) {
                var merged = grid.getRegionContaining(table.getLeft() + col, table.getTop() + row);
                var rows = merged == null ? 1 : GridRegionUtils.height(merged);
                if (rows > 1 && !sameValue(tableView.rules, title, row - 1, rows)) {
                    removeMergedRegions(tableBody, merged);
                }
                row += rows;
            }
        }
    }

    /** Whether the rules from the first one on give the column one value, a missing rule giving none. */
    private static boolean sameValue(List<? extends Map<String, Object>> rules, String title, int first, int count) {
        var value = valueOf(rules, title, first);
        return IntStream.range(first + 1, first + count).allMatch(i -> Objects.equals(valueOf(rules, title, i), value));
    }

    private static @Nullable Object valueOf(List<? extends Map<String, Object>> rules, String title, int index) {
        return index < rules.size() ? rules.get(index).get(title) : null;
    }

    private void writeConditionHeaders(IGridTable tableBody, SimpleRulesView tableView) {
        var col = 0;
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
            var tableBody = getGridTable(IXlsTableNames.VIEW_BUSINESS);
            var headers = SimpleRulesTableReader
                    .getConditionHeaders(LogicalTableHelper.logicalTable(tableBody.getRow(0)));
            var row = GridRegionUtils.height(tableBody.getRegion());
            for (var rule : tableAppend.getRules()) {
                var col = 0;
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
        var type = SimpleRulesView.TABLE_TYPE;
        return Boolean.TRUE.equals(tableView.collect) ? type + " " + IXlsTableNames.COLLECT : type;
    }
}
