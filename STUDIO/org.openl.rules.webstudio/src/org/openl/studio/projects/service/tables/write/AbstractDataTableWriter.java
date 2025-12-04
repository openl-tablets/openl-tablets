package org.openl.studio.projects.service.tables.write;

import java.util.Collection;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.GridTool;
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
 * <p>
 * Subclasses only need to implement updateHeader() for their specific header format.
 *
 * @author Vladyslav Pikus
 */
public abstract class AbstractDataTableWriter<T extends AbstractDataView> extends TableWriter<T> {

    public AbstractDataTableWriter(IOpenLTable table) {
        super(table);
    }

    public AbstractDataTableWriter(IGridTable gridTable, MetaInfoWriter metaInfoWriter) {
        super(gridTable, metaInfoWriter);
    }

    /**
     * Update business body with display names and data rows.
     * Uses getGridTable(VIEW_BUSINESS) which contains:
     * Row 0: Display names
     * Row 1+: Data rows
     */
    @Override
    protected final void updateBusinessBody(T tableView) {
        var tableBody = getGridTable(IXlsTableNames.VIEW_BUSINESS);

        int col = 0;
        boolean hasForeignKeys = hasForeignKeysInHeaders(tableView.headers);
        int displayValueRowIndex = hasForeignKeys ? 2 : 1;
        for (var header : tableView.headers) {
            createOrUpdateCell(tableBody, buildCellKey(col, 0), header.fieldName);
            if (hasForeignKeys) {
                String value = null;
                if (StringUtils.isNotBlank(header.foreignKey)) {
                    value = ">" + header.foreignKey;
                }
                createOrUpdateCell(tableBody, buildCellKey(col, 1), value);
            }
            createOrUpdateCell(tableBody, buildCellKey(col, displayValueRowIndex), header.displayName);
            col++;
        }

        // Write Data rows
        int nextRow = writeDataRowsToBusinessBody(tableBody, displayValueRowIndex + 1, tableView.rows);

        if (isUpdateMode()) {
            // Clean up removed rows
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
    }

    /**
     * Check if any header has a foreign key
     */
    protected boolean hasForeignKeysInHeaders(Collection<DataHeaderView> headers) {
        return headers.stream().anyMatch(h -> StringUtils.isNotBlank(h.foreignKey));
    }

    /**
     * Write all data rows to business body starting at row 1
     */
    protected int writeDataRowsToBusinessBody(IGridTable tableBody, int fromRow, Collection<DataRowView> rows) {
        int row = fromRow;
        for (var dataRow : rows) {
            writeDataRowToBusinessBody(tableBody, row, dataRow);
            row++;
        }
        return row;
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
     * Append new rows to the table
     * Must be implemented by subclasses to handle their specific append request type
     */
    protected void appendRows(Collection<DataRowView> rows) {
        if (!isUpdateMode()) {
            throw new IllegalStateException("Append operation is only allowed in update mode.");
        }
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

    @Override
    protected IGridTable getGridTable(String view) {
        if (IXlsTableNames.VIEW_BUSINESS.equals(view)) {
            // manually calculate business body ONLY without property section
            // business body of originalTable does not include rows with field name and foreign key info
            var gridRegion = originalTable.getRegion();
            var originalGrid = originalTable.getGrid();
            int leftCell = gridRegion.getLeft();
            int topCell = gridRegion.getTop();
            int firstPropertyRow = IGridRegion.Tool.height(originalGrid.getCell(leftCell, topCell).getAbsoluteRegion());
            String propsHeader = originalGrid.getCell(leftCell, topCell + firstPropertyRow).getStringValue();
            int fromRow = 1;
            if (!GridTool.tableWithoutPropertySection(propsHeader)) {
                int propsCount = originalGrid.getCell(leftCell, topCell + 1).getHeight();
                fromRow += propsCount;
            }
            return originalTable.getSubtable(0,
                    fromRow,
                    originalTable.getWidth(),
                    originalTable.getHeight() - fromRow);
        } else {
            return originalTable;
        }
    }

}
