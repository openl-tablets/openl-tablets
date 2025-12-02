package org.openl.studio.projects.service.tables.write;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.studio.projects.model.tables.SmartRulesAppend;
import org.openl.studio.projects.model.tables.SmartRulesHeaderView;
import org.openl.studio.projects.model.tables.SmartRulesView;
import org.openl.studio.projects.service.tables.read.SmartRulesTableReader;

/**
 * Writes {@link SmartRulesView} model to {@code SmartRules} table.
 *
 * @author Vladyslav Pikus
 */
public class SmartRulesWriter extends ExecutableTableWriter<SmartRulesView> {

    public SmartRulesWriter(IOpenLTable table) {
        super(table);
    }

    @Override
    protected void updateBusinessBody(SmartRulesView tableView) {
        var tableBody = table.getGridTable(IXlsTableNames.VIEW_BUSINESS);
        writeConditionHeaders(tableBody, tableView);
        int row = 1;
        int colMax = 0;
        for (var rule : tableView.rules) {
            int col = 0;
            for (var header : tableView.headers) {
                var value = rule.get(header.title);
                if (header.width == 2) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = value instanceof Map ? (Map<String, Object>) value : Map.of();
                    createOrUpdateCell(tableBody,
                            buildCellKey(col++, row),
                            map.get(SmartRulesTableReader.RANGE_RULE_MIN));
                    createOrUpdateCell(tableBody,
                            buildCellKey(col++, row),
                            map.get(SmartRulesTableReader.RANGE_RULE_MAX));
                } else {
                    createOrUpdateCell(tableBody, buildCellKey(col++, row), value);
                }
            }
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

    private void writeConditionHeaders(IGridTable tableBody, SmartRulesView tableView) {
        int col = 0;
        for (var header : tableView.headers) {
            updateHeaderWidth(header, tableView.rules);
            for (int i = 0; i < header.width; i++) {
                createOrUpdateCell(tableBody, buildCellKey(col, 0), header.title);
                col++;
            }
        }
    }

    private void updateHeaderWidth(SmartRulesHeaderView header, List<LinkedHashMap<String, Object>> rules) {
        for (var rule : rules) {
            var value = rule.get(header.title);
            if (value != null) {
                if (value instanceof Map) {
                    header.width = 2;
                }
                break;
            }
        }
    }

    public void append(SmartRulesAppend tableAppend) {
        try {
            table.getGridTable().edit();
            var tableBody = table.getGridTable(IXlsTableNames.VIEW_BUSINESS);
            var headers = SmartRulesTableReader
                    .getConditionHeaders(LogicalTableHelper.logicalTable(tableBody.getRow(0)));
            int row = IGridRegion.Tool.height(tableBody.getRegion());
            for (var rule : tableAppend.getRules()) {
                int col = 0;
                for (var header : headers) {
                    var value = rule.get(header.title);
                    if (header.width == 2) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = value instanceof Map ? (Map<String, Object>) value : Map.of();
                        createOrUpdateCell(tableBody,
                                buildCellKey(col++, row),
                                map.get(SmartRulesTableReader.RANGE_RULE_MIN));
                        createOrUpdateCell(tableBody,
                                buildCellKey(col++, row),
                                map.get(SmartRulesTableReader.RANGE_RULE_MAX));
                    } else {
                        createOrUpdateCell(tableBody, buildCellKey(col++, row), value);
                    }
                }
                row++;
            }
            save();
        } finally {
            table.getGridTable().stopEditing();
        }
    }

    @Override
    protected String getBusinessTableType(SmartRulesView tableView) {
        return SmartRulesView.TABLE_TYPE;
    }
}
