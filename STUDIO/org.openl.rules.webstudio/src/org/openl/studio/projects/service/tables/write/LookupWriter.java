package org.openl.studio.projects.service.tables.write;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.studio.projects.model.tables.LookupAppend;
import org.openl.studio.projects.model.tables.LookupHeaderView;
import org.openl.studio.projects.model.tables.LookupView;
import org.openl.studio.projects.service.tables.read.LookupTableReader;

/**
 * Writes {@link LookupView} model to {@code SmartLookup} or {@code SimpleLookup} table.
 *
 * @author Vladyslav Pikus
 */
public class LookupWriter extends ExecutableTableWriter<LookupView> {

    public LookupWriter(IOpenLTable table) {
        super(table);
    }

    @Override
    protected void mergeHeaderCells(LookupView tableView) {
    }

    @Override
    protected void updateBusinessBody(LookupView tableView) {
        var tableBody = table.getGridTable(IXlsTableNames.VIEW_BUSINESS);
        var rowNum = getHeaderDepth(tableView.headers);
        var colMax = writeSubHeader(tableBody, tableView.headers, 0, 0);

        rowNum = writeRows(tableBody, tableView.headers, tableView.rows, rowNum);

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
        Object[] values = new Object[maxHeaderWidth(header)];
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
        for (var header : headers) {
            var width = maxHeaderWidth(header);
            for (int i = 0; i < width; i++) {
                createOrUpdateCell(tableBody, buildCellKey(col, fromRow), header.title);
                col++;
            }
            writeSubHeader(tableBody, header.children, col - width, fromRow + 1);
        }
        return col;
    }

    private int maxHeaderWidth(LookupHeaderView header) {
        if (header.children.isEmpty()) {
            return 1;
        }
        int totalWidth = 0;
        for (var child : header.children) {
            totalWidth += maxHeaderWidth(child);
        }
        return totalWidth;
    }

    /**
     * Calculates the maximum depth of the header hierarchy.
     */
    private int getHeaderDepth(List<LookupHeaderView> headers) {
        return headers.stream()
                .mapToInt(this::getHeaderDepthRecursive)
                .max()
                .orElse(1);
    }

    /**
     * Recursively calculates the depth of a header.
     */
    private int getHeaderDepthRecursive(LookupHeaderView header) {
        if (header.children.isEmpty()) {
            return 1;
        }
        return 1 + header.children.stream()
                .mapToInt(this::getHeaderDepthRecursive)
                .max()
                .orElse(0);
    }

    public void append(LookupAppend tableAppend) {
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
