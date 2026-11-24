package org.openl.rules.table;

import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Logical table model that delegate original table and access to original table through coordinates transformation.
 *
 * <p>A part of the table and the transposed table are transformed the same way, so their cells are read through the
 * transformation too: a cell the transformation empties or widens stays so in any part and in both orientations.
 *
 * @author PUdalau
 */
public class TransformedGridTable extends AGridTableDecorator {

    @Getter
    private final @NonNull CoordinatesTransformer transformer;
    /** Whether the table is a part of a transformed table, located on the sheet by its own cells. */
    private final boolean part;

    public TransformedGridTable(@NonNull IGridTable gridTable, @NonNull CoordinatesTransformer transformer) {
        this(gridTable, transformer, false);
    }

    private TransformedGridTable(@NonNull IGridTable gridTable,
                                 @NonNull CoordinatesTransformer transformer,
                                 boolean part) {
        super(gridTable);
        this.transformer = transformer;
        this.part = part;
    }

    @Override
    public @NonNull ICell getCell(int column, int row) {
        var cell = table.getCell(getColumn(column, row), getRow(column, row));
        var empty = transformer.isEmpty(column, row);
        var width = transformer.getCellWidth(column, row);
        var height = transformer.getCellHeight(column, row);
        if (empty || width > 0 || height > 0) {
            return new TransformedCell(cell, empty, width, height);
        }
        return cell;
    }

    @Override
    public String getUri() {
        return part ? super.getUri() : table.getGrid().getUri();
    }

    private int getRow(int col, int row) {
        return transformer.getRow(col, row);
    }

    private int getColumn(int col, int row) {
        return transformer.getColumn(col, row);
    }

    @Override
    public int getWidth() {
        return transformer.getWidth();
    }

    @Override
    public int getHeight() {
        return transformer.getHeight();
    }

    @Override
    public int getGridRow(int column, int row) {
        return table.getGridRow(getColumn(column, row), getRow(column, row));
    }

    @Override
    public int getGridColumn(int column, int row) {
        return table.getGridColumn(getColumn(column, row), getRow(column, row));
    }

    @Override
    public boolean isNormalOrientation() {
        return table.isNormalOrientation();
    }

    @Override
    public @NonNull IGridTable transpose() {
        return new TransformedGridTable(table.transpose(), new Transposed(transformer), part);
    }

    @Override
    public @Nullable IGridTable getSubtable(int column, int row, int width, int height) {
        if (width == 0 || height == 0) {
            return null;
        }
        if (column == 0 && row == 0 && width == getWidth() && height == getHeight()) {
            return this;
        }
        var part = transformer instanceof Part outer
                ? new Part(outer.transformer(), outer.left() + column, outer.top() + row, width, height)
                : new Part(transformer, column, row, width, height);
        return new TransformedGridTable(table, part, true);
    }

    /**
     * The transformation of a part of the table: the cells from its left column and its top row.
     */
    private record Part(@NonNull CoordinatesTransformer transformer, int left, int top, int width, int height)
            implements CoordinatesTransformer {

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getColumn(int col, int row) {
            return transformer.getColumn(left + col, top + row);
        }

        @Override
        public int getRow(int col, int row) {
            return transformer.getRow(left + col, top + row);
        }

        @Override
        public boolean isEmpty(int col, int row) {
            return transformer.isEmpty(left + col, top + row);
        }

        @Override
        public int getCellWidth(int col, int row) {
            return transformer.getCellWidth(left + col, top + row);
        }

        @Override
        public int getCellHeight(int col, int row) {
            return transformer.getCellHeight(left + col, top + row);
        }
    }

    /**
     * The transformation of the transposed table: its columns are the rows of the transformed table.
     */
    private record Transposed(@NonNull CoordinatesTransformer transformer) implements CoordinatesTransformer {

        @Override
        public int getHeight() {
            return transformer.getWidth();
        }

        @Override
        public int getWidth() {
            return transformer.getHeight();
        }

        @Override
        public int getColumn(int col, int row) {
            return transformer.getRow(row, col);
        }

        @Override
        public int getRow(int col, int row) {
            return transformer.getColumn(row, col);
        }

        @Override
        public boolean isEmpty(int col, int row) {
            return transformer.isEmpty(row, col);
        }

        @Override
        public int getCellWidth(int col, int row) {
            return transformer.getCellHeight(row, col);
        }

        @Override
        public int getCellHeight(int col, int row) {
            return transformer.getCellWidth(row, col);
        }
    }
}
