package org.openl.studio.projects.service.tables.write;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.studio.projects.model.tables.LookupAppend;
import org.openl.studio.projects.model.tables.LookupHeaderView;
import org.openl.studio.projects.model.tables.LookupView;
import org.openl.studio.projects.service.tables.read.LookupTableReader;
import org.openl.util.CollectionUtils;

/**
 * Writes {@link LookupView} model to {@code SmartLookup} or {@code SimpleLookup} table.
 *
 * @author Vladyslav Pikus
 */
public class LookupWriter extends ExecutableTableWriter<LookupView> {

    public LookupWriter(IOpenLTable table) {
        super(table);
    }

    public LookupWriter(IGridTable gridTable, MetaInfoWriter metaInfoWriter) {
        super(gridTable, metaInfoWriter);
    }

    @Override
    protected void mergeHeaderCells(LookupView tableView) {
        if (!isUpdateMode()) {
            int latestCol = tableView.headers.stream()
                    .mapToInt(h -> Math.max(h.getWidth(), 1))
                    .sum();
            if (CollectionUtils.isNotEmpty(tableView.properties)) {
                latestCol = Math.max(NUMBER_PROPERTIES_COLUMNS, latestCol);
            }
            var mergeTitleRegion = new GridRegion(0, 0, 0, latestCol - 1);
            applyMergeRegions(getGridTable(), List.of(mergeTitleRegion));
        }
    }

    @Override
    protected void updateBusinessBody(LookupView tableView) {
        var tableBody = getGridTable(IXlsTableNames.VIEW_BUSINESS);
        var rowNum = LookupHeaderView.getHeaderDepth(tableView.headers);
        var colMax = writeSubHeader(tableBody, tableView.headers, 0, 0);

        rowNum = writeRows(tableBody, tableView.headers, tableView.rows, rowNum);

        if (isUpdateMode()) {
            // clean up removed columns
            var width = IGridRegion.Tool.width(tableBody.getRegion());
            if (colMax < width) {
                removeColumns(tableBody, width - colMax, colMax);
            }

            // clean up removed rows
            var height = IGridRegion.Tool.height(tableBody.getRegion());
            if (rowNum < height) {
                removeRows(tableBody, height - rowNum, rowNum);
            }
        }
    }

    /**
     * Writes rows of data to the table body.
     *
     * @param tableBody the grid table body
     * @param headers the column headers
     * @param rows the data rows to write
     * @param startRow the starting row number
     */
    private int writeRows(IGridTable tableBody,
                           List<LookupHeaderView> headers,
                           List<LinkedHashMap<String, Object>> rows,
                           int startRow) {
        int rowNum = startRow;
        for (var row : rows) {
            int col = 0;
            for (var header : headers) {
                var values = extractValues(header, row);
                for (var cellValue : values) {
                    createOrUpdateCell(tableBody, buildCellKey(col++, rowNum), cellValue);
                }
            }
            rowNum++;
        }
        return rowNum;
    }

    @SuppressWarnings("unchecked")
    private Object[] extractValues(LookupHeaderView header, Map<String, Object> row) {
        Object[] values = new Object[header.getWidth()];
        if (header.children.isEmpty()) {
            values[0] = row.get(header.title);
        } else {
            int index = 0;
            Map<String, Object> subRow = (Map<String, Object>) row.get(header.title);
            for (var child : header.children) {
                Object[] childValues = extractValues(child, subRow);
                System.arraycopy(childValues, 0, values, index, childValues.length);
                index += childValues.length;
            }
        }
        return values;
    }

    private int writeSubHeader(IGridTable tableBody, List<LookupHeaderView>  headers, int fromCol, int fromRow) {
        int col = fromCol;
        int maxHeight = LookupHeaderView.getHeaderDepth(headers);
        List<IGridRegion> mergeRegions = new ArrayList<>();
        for (var header : headers) {
            var width = header.getWidth();
            int left = col;
            int right = left + width - 1;
            for (int c = left; c <= right; c++) {
                createOrUpdateCell(tableBody, buildCellKey(c, fromRow), header.title);
            }
            if (!header.children.isEmpty()) {
                writeSubHeader(tableBody, header.children, left, fromRow + 1);
            }
            if (width > 1 || maxHeight > 1) {
                int bottom = header.children.isEmpty()
                        ? fromRow + maxHeight - 1 // merge vertically only if no children
                        : fromRow;
                var mergeCondition = new GridRegion(fromRow, left, bottom, right);
                mergeRegions.add(mergeCondition);
            }
            col = right + 1;
        }
        applyMergeRegions(tableBody, mergeRegions);
        return col;
    }

    public void append(LookupAppend tableAppend) {
        if (!isUpdateMode()) {
            throw new IllegalStateException("Append operation is only allowed in update mode.");
        }
        try {
            table.getGridTable().edit();
            var tableBody = table.getGridTable(IXlsTableNames.VIEW_BUSINESS);
            var headerTable = LogicalTableHelper.logicalTable(tableBody.getRow(0));
            List<LookupHeaderView> headers = LookupTableReader.buildHeaders(headerTable);

            // Find the starting row for appending (after header rows and existing data)
            int rowNum = IGridRegion.Tool.height(tableBody.getRegion());

            // Append new rows using shared logic
            writeRows(tableBody, headers, tableAppend.getRows(), rowNum);
            save();
        } finally {
            table.getGridTable().stopEditing();
        }
    }

    @Override
    protected String getBusinessTableType(LookupView tableView) {
        return tableView.tableType;
    }
}
