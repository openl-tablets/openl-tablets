package org.openl.studio.projects.service.tables.write;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.AbstractDataView;
import org.openl.studio.projects.model.tables.DataHeaderView;
import org.openl.studio.projects.model.tables.DataRowView;
import org.openl.util.StringUtils;

/**
 * Abstract base class for writing Data and Test table structures.
 * Provides common logic for updating table headers, business body, and appending rows.
 *
 * Subclasses only need to implement updateHeader() for their specific header format.
 *
 * @author Vladyslav Pikus
 */
public abstract class AbstractDataTableWriter<T extends AbstractDataView> extends TableWriter<T> {

    public AbstractDataTableWriter(IOpenLTable table) {
        super(table);
    }

    /**
     * Update business body with display names and data rows.
     * Uses getGridTable(VIEW_BUSINESS) which contains:
     * Row 0: Display names
     * Row 1+: Data rows
     */
    @Override
    protected final void updateBusinessBody(T tableView) {
        var tableBody = table.getGridTable(IXlsTableNames.VIEW_BUSINESS);

        // VIEW_BUSINESS only contains display names (row 0) and data rows (row 1+)
        // Row 0: Display names
        writeDisplayNamesToBusinessBody(tableBody, tableView.headers);
        // Row 1+: Data rows
        writeDataRowsToBusinessBody(tableBody, tableView.rows);

        // Clean up removed rows
        int nextRow = 1 + tableView.rows.size();
        var height = IGridRegion.Tool.height(tableBody.getRegion());
        if (nextRow < height) {
            removeRows(tableBody, height - nextRow, nextRow);
        }

        // Clean up removed columns
        int width = tableView.headers.size();
        var currentWidth = IGridRegion.Tool.width(tableBody.getRegion());
        if (width < currentWidth) {
            removeColumns(tableBody, currentWidth - width, width);
        }
    }

    /**
     * Check if any header has a foreign key
     */
    protected boolean hasForeignKeysInHeaders(java.util.Collection<DataHeaderView> headers) {
        return headers.stream().anyMatch(h -> StringUtils.isNotBlank(h.foreignKey));
    }

    /**
     * Write field names to full table body (row 0)
     */
    protected void writeFieldNamesToFullBody(IGridTable fullTableBody, java.util.Collection<DataHeaderView> headers) {
        int col = 0;
        for (var header : headers) {
            createOrUpdateCell(fullTableBody, buildCellKey(col, 0), header.fieldName);
            col++;
        }
    }

    /**
     * Write foreign keys to full table body (row 1) - with '>' prefix
     */
    protected void writeForeignKeysToFullBody(IGridTable fullTableBody, java.util.Collection<DataHeaderView> headers) {
        int col = 0;
        for (var header : headers) {
            String value = null;
            if (StringUtils.isNotBlank(header.foreignKey)) {
                value = ">" + header.foreignKey;
            }
            createOrUpdateCell(fullTableBody, buildCellKey(col, 1), value);
            col++;
        }
    }

    /**
     * Write display names to business body (row 0)
     */
    protected void writeDisplayNamesToBusinessBody(IGridTable tableBody, java.util.Collection<DataHeaderView> headers) {
        int col = 0;
        for (var header : headers) {
            createOrUpdateCell(tableBody, buildCellKey(col, 0), header.displayName);
            col++;
        }
    }

    /**
     * Write all data rows to business body starting at row 1
     */
    protected void writeDataRowsToBusinessBody(IGridTable tableBody, java.util.Collection<DataRowView> rows) {
        int row = 1;
        for (var dataRow : rows) {
            writeDataRowToBusinessBody(tableBody, row, dataRow);
            row++;
        }
    }

    /**
     * Write a single data row to business body
     */
    protected void writeDataRowToBusinessBody(IGridTable tableBody, int row, DataRowView dataRow) {
        int col = 0;
        for (var value : dataRow.values) {
            createOrUpdateCell(tableBody, buildCellKey(col, row), value);
            col++;
        }
    }

    /**
     * Update all table header rows (field names and foreign keys)
     * Uses getSyntaxNode().getTableBody() to access the full table structure including headers
     */
    protected final void updateTableHeaders(T tableView) {
        var tsn = table.getSyntaxNode();
        var logicalTableBody = tsn.getTableBody();
        if (logicalTableBody == null) {
            return;
        }

        // Get the underlying grid table for updates
        IGridTable fullTableBody = logicalTableBody.getSource();
        if (fullTableBody == null) {
            return;
        }

        boolean hasForeignKeys = hasForeignKeysInHeaders(tableView.headers);

        // Write field names to row 0
        writeFieldNamesToFullBody(fullTableBody, tableView.headers);

        // Write foreign keys to row 1 if needed
        if (hasForeignKeys) {
            writeForeignKeysToFullBody(fullTableBody, tableView.headers);
        }
    }

    /**
     * Append new rows to the table
     * Must be implemented by subclasses to handle their specific append request type
     */
    protected void appendRows(java.util.Collection<DataRowView> rows) {
        try {
            table.getGridTable().edit();
            var tableBody = table.getGridTable(IXlsTableNames.VIEW_BUSINESS);

            // Calculate next row after existing data
            // The height already points to the next empty row after all existing data
            int row = IGridRegion.Tool.height(tableBody.getRegion());

            // Append new rows
            for (var dataRow : rows) {
                writeDataRowToBusinessBody(tableBody, row, dataRow);
                row++;
            }

            save();
        } finally {
            table.getGridTable().stopEditing();
        }
    }

}
