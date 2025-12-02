package org.openl.studio.projects.service.tables.read;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.openl.rules.table.ICell;
import org.openl.rules.table.ITable;
import org.openl.studio.projects.model.tables.AbstractDataView;
import org.openl.studio.projects.model.tables.DataHeaderView;
import org.openl.studio.projects.model.tables.DataRowView;
import org.openl.util.StringUtils;

/**
 * Abstract base class for reading table structures similar to Data/Test tables.
 * Provides common logic for parsing headers and rows with optional foreign keys.
 *
 * @author Vladyslav Pikus
 */
public abstract class AbstractDataTableReader<T extends AbstractDataView, R extends AbstractDataView.Builder<R>>
        extends EditableTableReader<T, R> {

    public AbstractDataTableReader(java.util.function.Supplier<R> builderCreator) {
        super(builderCreator);
    }

    /**
     * Determines where data rows start based on table structure.
     * Row 0: Field names
     * Row 1: Either foreign keys (if any cell starts with >) or display names
     * Row 2: Either display names (if row 1 has foreign keys) or first data row
     * Row 3+: Data rows
     *
     * @param tableBody table body
     * @return row index where data starts
     */
    protected int determineDataStartRow(ITable<?> tableBody) {
        if (tableBody.getHeight() < 2) {
            return 2; // Not enough rows, assume data starts at row 2
        }

        // Check row 1 for foreign keys
        var hasForeignKeys = hasForeignKeysInRow(tableBody);

        // If row 1 has foreign keys: display names in row 2, data starts at row 3
        // Otherwise: display names in row 1, data starts at row 2
        return hasForeignKeys ? 3 : 2;
    }

    /**
     * Check if any cell in the given row starts with '>' (indicates foreign key)
     */
    protected boolean hasForeignKeysInRow(ITable<?> tableBody) {
        int width = tableBody.getWidth();
        for (int col = 0; col < width; col++) {
            var cell = tableBody.getCell(col, 1);
            var value = cell.getStringValue();
            if (value != null && value.startsWith(">")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Read column headers with field names, optional foreign keys, and display names.
     * <p>
     * Row 0: Field names
     * Row 1: Either foreign keys (starts with >) or display names
     * Row 2: Display names if row 1 has foreign keys
     */
    protected List<DataHeaderView> readHeaders(ITable<?> tableBody) {
        List<DataHeaderView> headers = new ArrayList<>();
        int width = tableBody.getWidth();

        boolean hasForeignKeys = hasForeignKeysInRow(tableBody);

        for (int col = 0; col < width; col++) {
            var fieldNameCell = tableBody.getCell(col, 0);
            var fieldName = fieldNameCell.getStringValue();

            String foreignKey = null;
            String displayName = null;

            if (hasForeignKeys) {
                // Row 1 contains foreign keys
                var foreignKeyCell = tableBody.getCell(col, 1);
                var foreignKeyValue = foreignKeyCell.getStringValue();
                if (foreignKeyValue != null && foreignKeyValue.startsWith(">")) {
                    foreignKey = foreignKeyValue.substring(1).trim(); // Remove '>' prefix
                }

                // Row 2 contains display names
                if (tableBody.getHeight() > 2) {
                    var displayNameCell = tableBody.getCell(col, 2);
                    displayName = displayNameCell.getStringValue();
                }
            } else {
                // Row 1 contains display names
                var displayNameCell = tableBody.getCell(col, 1);
                displayName = displayNameCell.getStringValue();
            }

            headers.add(DataHeaderView.builder()
                    .fieldName(StringUtils.trimToNull(fieldName))
                    .foreignKey(StringUtils.trimToNull(foreignKey))
                    .displayName(StringUtils.trimToNull(displayName))
                    .build());
        }

        return headers;
    }

    /**
     * Read data rows starting from the determined row
     */
    protected List<DataRowView> readRows(ITable<?> tableBody, int startRow, Function<ICell, Object> cellValueReader) {
        List<DataRowView> rows = new ArrayList<>();
        int height = tableBody.getHeight();
        int width = tableBody.getWidth();

        for (int row = startRow; row < height; row++) {
            List<Object> values = new ArrayList<>();
            for (int col = 0; col < width; col++) {
                var cell = tableBody.getCell(col, row);
                values.add(cellValueReader.apply(cell));
            }
            rows.add(DataRowView.builder().values(values).build());
        }

        return rows;
    }

    /**
     * Read table body structure (headers and rows) and set them to builder
     * This is the common logic used by both DataTableReader and TestTableReader
     */
    protected final void readAndSetTableBody(R builder, ITable<?> tableBody, CellValueReader cellValueReader) {
        var headers = readHeaders(tableBody);
        var dataStartRow = determineDataStartRow(tableBody);
        var rows = readRows(tableBody, dataStartRow, cellValueReader);

        builder.headers(headers);
        builder.rows(rows);
    }

}
