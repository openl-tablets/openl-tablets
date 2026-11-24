package org.openl.rules.table;

import java.util.Date;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.ui.TextRun;

/**
 * A cell of a {@link TransformedGridTable} that the transformation empties or resizes.
 *
 * <p>The cell stands where its cell of the source table stands on the sheet, and keeps its style.
 *
 * <p>An empty cell gives no value, no formula and no comment, and is merged with no other cell. A resized cell spans
 * the columns and rows the transformation gives it; a size of {@code 0} keeps the size of the cell of the source
 * table.
 */
@RequiredArgsConstructor
final class TransformedCell implements ICell {

    private final @NonNull ICell cell;
    private final boolean empty;
    private final int width;
    private final int height;

    @Override
    public int getRow() {
        return cell.getRow();
    }

    @Override
    public int getColumn() {
        return cell.getColumn();
    }

    @Override
    public int getAbsoluteRow() {
        return cell.getAbsoluteRow();
    }

    @Override
    public int getAbsoluteColumn() {
        return cell.getAbsoluteColumn();
    }

    @Override
    public @NonNull IGridRegion getAbsoluteRegion() {
        return cell.getAbsoluteRegion();
    }

    @Override
    public int getWidth() {
        return width > 0 ? width : sizeOf(cell.getWidth());
    }

    @Override
    public int getHeight() {
        return height > 0 ? height : sizeOf(cell.getHeight());
    }

    /** An empty cell covers one place only, whatever its cell of the source table spans. */
    private int sizeOf(int sourceSize) {
        return empty ? 1 : sourceSize;
    }

    @Override
    public @Nullable ICellStyle getStyle() {
        return cell.getStyle();
    }

    @Override
    public @Nullable Object getObjectValue() {
        return empty ? null : cell.getObjectValue();
    }

    @Override
    public @Nullable String getStringValue() {
        return empty ? null : cell.getStringValue();
    }

    @Override
    public @Nullable ICellFont getFont() {
        return cell.getFont();
    }

    @Override
    public @NonNull List<TextRun> getTextRuns() {
        return empty ? List.of() : cell.getTextRuns();
    }

    @Override
    public @Nullable IGridRegion getRegion() {
        return empty ? null : cell.getRegion();
    }

    @Override
    public @Nullable String getFormula() {
        return empty ? null : cell.getFormula();
    }

    @Override
    public int getType() {
        return empty ? IGrid.CELL_TYPE_BLANK : cell.getType();
    }

    @Override
    public @NonNull String getUri() {
        return cell.getUri();
    }

    @Override
    public boolean hasNativeType() {
        return !empty && cell.hasNativeType();
    }

    @Override
    public int getNativeType() {
        return empty ? IGrid.CELL_TYPE_BLANK : cell.getNativeType();
    }

    @Override
    public double getNativeNumber() {
        return empty ? 0 : cell.getNativeNumber();
    }

    @Override
    public boolean getNativeBoolean() {
        return !empty && cell.getNativeBoolean();
    }

    @Override
    public @Nullable Date getNativeDate() {
        return empty ? null : cell.getNativeDate();
    }

    @Override
    public @Nullable ICellComment getComment() {
        return empty ? null : cell.getComment();
    }

    @Override
    public @NonNull ICell getTopLeftCellFromRegion() {
        return empty ? this : cell.getTopLeftCellFromRegion();
    }
}
