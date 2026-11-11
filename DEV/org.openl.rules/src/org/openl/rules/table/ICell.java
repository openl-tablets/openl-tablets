package org.openl.rules.table;

import java.util.Date;
import java.util.List;

import org.jspecify.annotations.NonNull;

import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.ui.TextRun;
import org.openl.rules.table.xls.XlsUtil;

public interface ICell {

    int getRow();

    int getColumn();

    /**
     * @return Absolute row index inside the sheet.
     */
    int getAbsoluteRow();

    /**
     * @return Absolute column index inside the sheet.
     */
    int getAbsoluteColumn();

    /**
     * The region of the sheet this cell covers: the merge it belongs to, or the one square it stands on where
     * it belongs to none. Never {@code null} — a cell always covers somewhere.
     *
     * @return Absolute region of cell inside the sheet.
     */
    @NonNull
    IGridRegion getAbsoluteRegion();

    int getWidth();

    int getHeight();

    ICellStyle getStyle();

    Object getObjectValue();

    String getStringValue();

    // Kept as an open refactoring task: it needs a design change beyond this cleanup.
    @SuppressWarnings("java:S1135")
    // TODO: move this method to ICellStyle
    ICellFont getFont();

    /**
     * Returns the pieces of the cell text that are formatted with fonts of their own.
     *
     * <p>A cell whose text takes the font of the cell returns an empty list. Its text is drawn with
     * {@link #getFont()}.
     *
     * @return the runs of the cell text, or an empty list when the text takes the font of the cell
     */
    default @NonNull List<TextRun> getTextRuns() {
        return List.of();
    }

    /**
     * @return grid region, if cell belongs to any merged region. In other cases <code>null</code>.
     */
    IGridRegion getRegion();

    String getFormula();

    /**
     * Returns one of IGrid.CELL_TYPE_* values.
     */
    int getType();

    /**
     * Returns the address of the cell in its sheet, such as {@code B3}.
     */
    default String getUri() {
        return XlsUtil.xlsCellPresentation(getAbsoluteColumn(), getAbsoluteRow());
    }

    // used for optimized access

    /**
     * Tells whether the cell gives fast access to its native value. The native methods are not used on a cell that
     * cannot.
     *
     * <p>A cell gives the access unless it tells otherwise.
     */
    default boolean hasNativeType() {
        return true;
    }

    /**
     * Returns the type of the native value, one of the {@code IGrid.CELL_TYPE_*} values. A formula cell returns the
     * type of its cached value.
     *
     * <p>A cell returns its {@link #getType() type} unless it tells otherwise.
     */
    default int getNativeType() {
        return getType();
    }

    double getNativeNumber();

    boolean getNativeBoolean();

    Date getNativeDate();

    ICellComment getComment();

    ICell getTopLeftCellFromRegion();

}
