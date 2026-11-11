package org.openl.studio.projects.service.tables.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.jspecify.annotations.Nullable;

import org.openl.rules.table.GridTable;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.ui.TextRun;
import org.openl.rules.table.xls.PoiExcelHelper.ThemedColor;
import org.openl.studio.projects.model.tables.RawTableRegion;
import org.openl.studio.projects.service.tables.format.TableStyle.Line;
import org.openl.studio.projects.service.tables.format.TableStyle.Look;
import org.openl.studio.projects.service.tables.format.WorkbookStyles.Edges;

/**
 * A grid that shows one of its tables in the OpenL table style, as {@link ExcelTableFormatter} writes the style into a
 * workbook. It serves a table whose file keeps no formatting, a table of a Markdown module: the table is read as if it
 * were formatted, and nothing is written into its file.
 *
 * <p>Each cell of the table takes the look of its role and the lines at the edges of its region, and the header text
 * is written in its pieces. The values of the cells, and every cell outside the table, are those of the grid.
 *
 * <p>The colours are the colours of the Office theme, the theme a workbook with no theme gets. A screen draws a table
 * in its own font, so the font of a cell names no face and no size.
 */
public final class StyledGrid implements IGrid {

    @Delegate(excludes = Cells.class)
    private final IGrid grid;
    private final IGridRegion table;
    private final TableRegions regions;
    private final Set<Integer> groupRows;
    private final Set<Integer> groupColumns;

    private StyledGrid(IGrid grid, IGridRegion table, TableRegions regions) {
        this.grid = grid;
        this.table = table;
        this.regions = regions;
        groupRows = Set.copyOf(regions.groups().rows());
        groupColumns = Set.copyOf(regions.groups().columns());
    }

    /**
     * A table shown in the OpenL table style.
     *
     * @param table   the table as it stands on its grid, header included
     * @param regions the regions of the table, in table coordinates
     * @return the same table on a grid that shows it in the style
     */
    public static IGridTable of(IGridTable table, TableRegions regions) {
        var region = table.getRegion();
        return new GridTable(region, new StyledGrid(table.getGrid(), region, regions));
    }

    @Override
    public ICell getCell(int column, int row) {
        var cell = grid.getCell(column, row);
        var at = row - table.getTop();
        var across = column - table.getLeft();
        return regions.regions()
                .stream()
                .filter(region -> region.holds(at, across))
                .findFirst()
                .<ICell>map(region -> styled(cell, region, at, across))
                .orElse(cell);
    }

    /** A cell of a region in the look of its role, standing at a place of the table. */
    private ICell styled(ICell cell, RawTableRegion region, int row, int column) {
        var style = TableStyle.of(region.role());
        var edges = Edges.of(style, region, row, groupRows, table.getBottom() - table.getTop());
        var look = style.getLook();
        // The header text is written in its pieces in the cell holding it only.
        var runs = row == 0 && column == 0 ? piecesOf(cell.getStringValue()) : List.<TextRun>of();
        return new StyledCell(cell, new RoleStyle(look, groupColumns.contains(column) ? edges.grouped(style) : edges),
                new RoleFont(look.bold(), look.italic(), look.colour()), runs);
    }

    /** The header text in its pieces, or no pieces when it is written as plain text. */
    private List<TextRun> piecesOf(@Nullable String header) {
        if (header == null || !regions.splits(header)) {
            return List.of();
        }
        var runs = new ArrayList<TextRun>();
        var from = 0;
        for (var piece : regions.signature()) {
            if (piece.to() > from) {
                var look = TableStyle.Piece.of(piece.part());
                runs.add(new TextRun(header.substring(from, piece.to()),
                        new RoleFont(look.bold(), false, look.colour())));
                from = piece.to();
            }
        }
        return runs;
    }

    /** The cells, which the grid shows in the style. */
    private interface Cells {
        ICell getCell(int column, int row);
    }

    /** What a cell is shown in: its style, its font and the pieces of its text. */
    private interface Shown {
        ICellStyle getStyle();

        ICellFont getFont();

        List<TextRun> getTextRuns();
    }

    /** A cell of the grid shown in the look of its role. */
    @Getter
    @RequiredArgsConstructor
    private static final class StyledCell implements ICell {

        @Delegate(excludes = Shown.class)
        @Getter(AccessLevel.NONE)
        private final ICell cell;
        private final ICellStyle style;
        private final ICellFont font;
        private final List<TextRun> textRuns;
    }

    /**
     * The style of a cell of a role.
     *
     * @param look  the look of the role
     * @param edges the lines at the edges of the region of the cell
     */
    private record RoleStyle(Look look, Edges edges) implements ICellStyle {

        @Override
        public BorderStyle[] getBorderStyle() {
            return new BorderStyle[]{styleOf(edges.top()), BorderStyle.NONE, styleOf(edges.bottom()),
                    styleOf(edges.left())};
        }

        @Override
        public short[][] getBorderRGB() {
            return new short[][]{rgbOf(edges.top()), rgbOf(null), rgbOf(edges.bottom()), rgbOf(edges.left())};
        }

        private static BorderStyle styleOf(@Nullable Line line) {
            return line == null ? BorderStyle.NONE : line.style();
        }

        /** The colour of a line, or black for a side with no line, as a workbook reads it. */
        private static short[] rgbOf(@Nullable Line line) {
            return line == null ? new short[3] : line.colour().toOfficeRgb();
        }

        // The array is one RGB colour, not a list: null stands for no colour, which table views keep as missing.
        @SuppressWarnings("java:S1168")
        @Override
        public short @Nullable [] getFillBackgroundColor() {
            return null;
        }

        @SuppressWarnings("java:S1168")
        @Override
        public short @Nullable [] getFillForegroundColor() {
            var fill = look.fill();
            return fill == null ? null : fill.toOfficeRgb();
        }

        @Override
        public short getFillBackgroundColorIndex() {
            return IndexedColors.AUTOMATIC.getIndex();
        }

        @Override
        public short getFillForegroundColorIndex() {
            return IndexedColors.AUTOMATIC.getIndex();
        }

        @Override
        public FillPatternType getFillPattern() {
            return look.fill() == null ? FillPatternType.NO_FILL : FillPatternType.SOLID_FOREGROUND;
        }

        @Override
        public HorizontalAlignment getHorizontalAlignment() {
            return look.align();
        }

        @Override
        public int getIndent() {
            return 0;
        }

        @Override
        public int getRotation() {
            return 0;
        }

        @Override
        public VerticalAlignment getVerticalAlignment() {
            return VerticalAlignment.CENTER;
        }

        @Override
        public boolean isWrappedText() {
            return false;
        }

        @Override
        public short getFormatIndex() {
            return 0;
        }

        @Override
        public String getFormatString() {
            return "General";
        }
    }

    /**
     * The font of a role, or of a piece of the header text.
     *
     * @param bold   whether the text is bold
     * @param italic whether the text is italic
     * @param colour the colour of the text
     */
    private record RoleFont(boolean bold, boolean italic, ThemedColor colour) implements ICellFont {

        @Override
        public short[] getFontColor() {
            return colour.toOfficeRgb();
        }

        @Override
        public @Nullable String getName() {
            return null;
        }

        @Override
        public int getSize() {
            return 0;
        }

        @Override
        public boolean isBold() {
            return bold;
        }

        @Override
        public boolean isItalic() {
            return italic;
        }

        @Override
        public boolean isStrikeout() {
            return false;
        }

        @Override
        public boolean isUnderlined() {
            return false;
        }
    }
}
