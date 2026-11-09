package org.openl.rules.table.xls;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.table.ICell;

/**
 * A sheet read as it stands, each of its cells read from the workbook once.
 *
 * <p>A cell keeps the value, the type and the formula it read first. The compiler reads the cells of a table several
 * times while it binds it, and a cell of a workbook loaded for writing is slow to read: this grid reads it once.
 *
 * <p>It serves to read a sheet that does not change while it is read, such as a table bound alone once it is written.
 * A write into the sheet is not seen by the cells read before it.
 */
public final class ReadOnceSheetGrid extends XlsSheetGridModel {

    private final Map<Long, ICell> cells = new HashMap<>();

    /**
     * Reads a sheet.
     *
     * @param sheetSource the sheet
     */
    public ReadOnceSheetGrid(XlsSheetSourceCodeModule sheetSource) {
        super(sheetSource);
    }

    @Override
    public ICell getCell(int column, int row) {
        return cells.computeIfAbsent(((long) row << 32) | (column & 0xFFFFFFFFL),
                key -> new ReadOnceCell(column, row, this));
    }

    /** A cell that reads what it holds from the workbook once. */
    private static final class ReadOnceCell extends XlsCell {

        private @Nullable Object value;
        private boolean valueRead;
        private int type = -1;
        private int nativeType = -1;
        private @Nullable String formula;
        private boolean formulaRead;

        ReadOnceCell(int column, int row, XlsSheetGridModel grid) {
            super(column, row, grid);
        }

        @Override
        public @Nullable Object getObjectValue() {
            if (!valueRead) {
                value = super.getObjectValue();
                valueRead = true;
            }
            return value;
        }

        @Override
        public int getType() {
            if (type < 0) {
                type = super.getType();
            }
            return type;
        }

        @Override
        public int getNativeType() {
            if (nativeType < 0) {
                nativeType = super.getNativeType();
            }
            return nativeType;
        }

        @Override
        public @Nullable String getFormula() {
            if (!formulaRead) {
                formula = super.getFormula();
                formulaRead = true;
            }
            return formula;
        }
    }
}
