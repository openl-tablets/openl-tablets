package org.openl.excel.parser.markdown;

import java.util.Date;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.rules.table.GridRegion;
import org.openl.rules.table.ICell;
import org.openl.rules.table.ICellComment;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.xls.XlsUtil;

/**
 * A cell of the sheet of a Markdown file opened for writing.
 *
 * <p>The cell holds text, or nothing. A cell a merged range covers reads as the top left cell of the range, and is one
 * cell wide and high, as a cell of a workbook is: the size of the range is that of its top left cell. The file keeps no
 * formatting, so the cell has no style, font, formula or comment.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class MarkdownCell implements ICell {

    @Getter
    private final int column;
    @Getter
    private final int row;
    private final MarkdownGrid grid;

    @Override
    public int getAbsoluteRow() {
        // The sheet of a file is its own grid: a cell stands where the grid has it
        return getRow();
    }

    @Override
    public int getAbsoluteColumn() {
        return getColumn();
    }

    @Override
    public @Nullable IGridRegion getRegion() {
        return grid.getRegionContaining(column, row);
    }

    @Override
    public IGridRegion getAbsoluteRegion() {
        var region = getRegion();
        return region == null ? new GridRegion(row, column, row, column) : region;
    }

    @Override
    public int getWidth() {
        var region = topLeftRegion();
        return region == null ? 1 : region.getRight() - region.getLeft() + 1;
    }

    @Override
    public int getHeight() {
        var region = topLeftRegion();
        return region == null ? 1 : region.getBottom() - region.getTop() + 1;
    }

    /**
     * Returns the merged range the cell opens, or {@code null} for a cell of its own or one a range covers.
     */
    private @Nullable IGridRegion topLeftRegion() {
        var region = getRegion();
        return region != null && region.getLeft() == column && region.getTop() == row ? region : null;
    }

    @Override
    public ICell getTopLeftCellFromRegion() {
        var region = getRegion();
        return region == null ? this : grid.getCell(region.getLeft(), region.getTop());
    }

    @Override
    public @Nullable Object getObjectValue() {
        var region = getRegion();
        return region == null ? grid.valueAt(column, row) : grid.valueAt(region.getLeft(), region.getTop());
    }

    @Override
    public @Nullable String getStringValue() {
        return (String) getObjectValue();
    }

    @Override
    public int getType() {
        return getObjectValue() == null ? IGrid.CELL_TYPE_BLANK : IGrid.CELL_TYPE_STRING;
    }

    @Override
    public String getUri() {
        return XlsUtil.xlsCellPresentation(column, row);
    }

    @Override
    public boolean hasNativeType() {
        return true;
    }

    @Override
    public int getNativeType() {
        return getType();
    }

    @Override
    public double getNativeNumber() {
        return getObjectValue() == null ? 0.0 : Double.NaN;
    }

    @Override
    public boolean getNativeBoolean() {
        return false;
    }

    @Override
    public @Nullable Date getNativeDate() {
        return null;
    }

    @Override
    public @Nullable ICellStyle getStyle() {
        return null;
    }

    @Override
    public @Nullable ICellFont getFont() {
        return null;
    }

    @Override
    public @Nullable String getFormula() {
        return null;
    }

    @Override
    public @Nullable ICellComment getComment() {
        return null;
    }
}
