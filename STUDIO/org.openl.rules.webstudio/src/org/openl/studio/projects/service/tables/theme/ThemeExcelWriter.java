package org.openl.studio.projects.service.tables.theme;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.extensions.XSSFCellBorder.BorderSide;
import org.jspecify.annotations.Nullable;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STCellType;

import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.table.GridTableUtils;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.rules.table.xls.PoiExcelHelper.FontAttributes;
import org.openl.rules.table.xls.PoiExcelHelper.ThemedColor;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.service.tables.theme.ThemedTable.ThemedCell;
import org.openl.studio.projects.service.tables.theme.ThemedTable.ThemedRun;
import org.openl.studio.projects.service.tables.write.TableWriter;

/**
 * Writes one table theme into the workbook.
 *
 * <p>Each cell keeps its own style and gets only the attributes the theme sets. Its number format, wrapping,
 * protection and every attribute the theme says nothing about stay as they are. A cell outside the table is never
 * touched. The text of a cell is never changed: the header is formatted in the pieces of the theme, and any other
 * text formatted in pieces of its own is written in the font of its cell where the theme names that font.
 *
 * <p>One writer serves a whole batch. A style or a font it makes once is reused by every cell that needs it. A font
 * the workbook already has is reused rather than made again, and a cell that already has the look keeps its style.
 * Writing the theme a second time therefore adds neither fonts nor styles.
 *
 * <p>A colour is compared as the workbook holds it. The palette of an {@code .xls} workbook may have no room for a
 * colour of the theme, which it then holds as the nearest colour it has: a cell holding that one has the look.
 *
 * <p>A colour the theme makes of a theme colour of Excel is written as that theme colour into a workbook whose theme
 * colours are those of the theme, so Excel offers it in its palette, and as {@code #rrggbb} into any other workbook.
 * A cell holding the colour the other way round has not the look. The theme of a workbook is never changed.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class ThemeExcelWriter {

    private final TableTheme theme;
    private final Map<Workbook, WorkbookLooks> workbooks = new IdentityHashMap<>();

    /**
     * Writes the theme into tables, notes the edit on each of them, and saves every workbook it reaches once.
     *
     * <p>A table of a kind no theme styles is left as it is, and so is its workbook when the theme reaches no other
     * table of it. A table the theme is written into is noted as edited, as any edit of a table is, after the theme:
     * a table without room for the note moves, so it is named by where it stands once written. A property the note
     * adds takes the theme too.
     *
     * @param tables the tables to theme
     * @param edit   what OpenL Studio notes about an edit of a table, such as who made it and when; empty to note
     *               nothing
     * @return the identifiers of the tables the theme was written into, as they stand once written
     */
    public List<String> writeAll(List<IOpenLTable> tables, Map<String, Object> edit) {
        var written = new ArrayList<String>();
        var edited = new ArrayList<IGridTable>();
        Map<XlsWorkbookSourceCodeModule, XlsSheetGridModel> saved = new IdentityHashMap<>();
        try {
            for (var table : tables) {
                var grid = GridTableUtils.getOriginalTable(table.getGridTable());
                grid.edit();
                edited.add(grid);
                if (write(table, grid, TableMoves.NONE)) {
                    noting(table, grid, () -> TableWriter.recordEdit(table, edit));
                    var sheet = (XlsSheetGridModel) grid.getGrid();
                    saved.putIfAbsent(sheet.getSheetSource().getWorkbookSource(), sheet);
                    written.add(TableWriter.tableIdOf(grid));
                }
            }
            saved.values().forEach(TableWriter::saveWorkbook);
        } finally {
            edited.forEach(IGridTable::stopEditing);
        }
        return written;
    }

    /**
     * Writes the theme into a table, as the table stands on its grid. The caller saves the workbook.
     *
     * <p>A table of a kind no theme styles is left as it is.
     *
     * @param table the table to theme
     * @param grid  the table as it stands on its sheet, header included; its sheet is opened for writing
     * @param moves the rows and the columns edits inserted into the table or deleted from it since it was compiled
     * @return whether the theme was written into the table
     */
    public boolean write(IOpenLTable table, IGridTable grid, TableMoves moves) {
        var layout = ThemeLayouts.of(table, grid, theme, moves);
        if (layout != null) {
            write(layout, grid, at -> true);
        }
        return layout != null;
    }

    /**
     * Notes an edit on a table the theme was written into, as a save of the table notes it.
     *
     * <p>The note is written after the theme. Each property it adds is a row of the properties of its own, and that
     * row takes the theme; a table without properties gets them so. The rest of the table is left as the theme and the
     * edits after it left it.
     *
     * @param table the table the theme was written into
     * @param grid  the table as it stands on its sheet, header included; its sheet is opened for writing
     * @param note  writes the note onto the table
     */
    public void noting(IOpenLTable table, IGridTable grid, Runnable note) {
        var before = rowsOf(propertiesOf(grid));
        note.run();
        var properties = propertiesOf(grid);
        var added = rowsOf(properties) - before;
        if (properties != null && added > 0) {
            // The note inserts each property it adds at the top of the properties, and the rows under them move down.
            var rows = properties.getSource().getRegion();
            // Only the header and the properties are laid out: the body under them keeps the look it has, so no part
            // the compiler found is looked for, and the edits that moved such parts do not matter.
            var head = grid.getSubtable(0, 0, grid.getWidth(), rows.getBottom() - grid.getRegion().getTop() + 1);
            var layout = ThemeLayouts.of(table, head, theme, TableMoves.NONE);
            if (layout != null) {
                write(layout, head, at -> at.row() >= rows.getTop() && at.row() < rows.getTop() + added);
            }
        }
    }

    /** The table properties a table declares under its header, or {@code null} for a table without them. */
    private static @Nullable ILogicalTable propertiesOf(IGridTable grid) {
        return PropertiesHelper.getPropertiesTableSection(LogicalTableHelper.logicalTable(grid));
    }

    /** How many rows of the sheet the table properties take: none for a table without them. */
    private static int rowsOf(@Nullable ILogicalTable properties) {
        return properties == null ? 0 : properties.getSource().getHeight();
    }

    /** Writes the look of a table into the cells the filter keeps. */
    private void write(ThemedTable layout, IGridTable grid, Predicate<ThemedTable.Cell> kept) {
        var sheet = ((XlsSheetGridModel) grid.getGrid()).getSheetToWrite();
        var colours = theme.themeColors();
        var looks = workbooks.computeIfAbsent(sheet.getWorkbook(),
                workbook -> new WorkbookLooks(workbook, colours != null && colours.areThoseOf(workbook)));
        layout.cells().forEach((at, themed) -> {
            if (kept.test(at)) {
                var cell = PoiExcelHelper.getOrCreateCell(at.column(), at.row(), sheet);
                var original = cell.getCellStyle();
                var style = looks.style(original, themed.style());
                // A cell that already has the look keeps its style untouched.
                if (style.getIndex() != original.getIndex()) {
                    cell.setCellStyle(style);
                }
                writeRuns(cell, themed, looks);
            }
        });
    }

    /**
     * Formats the header text in the pieces of the theme, and writes any other text in the font of its cell where the
     * theme names that font. A text whose font the theme says nothing of keeps its pieces. The text stays as it is.
     */
    private static void writeRuns(Cell cell, ThemedCell themed, WorkbookLooks looks) {
        if (cell.getCellType() != CellType.STRING) {
            return;
        }
        var own = cell.getRichStringCellValue();
        // Only the header is formatted in pieces of the theme, so the text of no other cell is read for them.
        List<ThemedRun> runs = themed.header() == null ? List.of() : themed.runs(own.getString());
        // Nothing to write: the theme formats no piece, and the text keeps the pieces it has or has none.
        if (runs.isEmpty() && (themed.keepsOwnRuns(runs) || own.numFormattingRuns() == 0)) {
            return;
        }
        var rich = cell.getSheet().getWorkbook().getCreationHelper().createRichTextString(own.getString());
        var cellFont = PoiExcelHelper.getCellFont(cell);
        runs.forEach(run -> rich.applyFont(run.start(), run.end(), looks.font(cellFont, run.style())));
        // A text written into the cell itself, as tools other than Excel write it, would keep its characters and
        // lose the runs. Blanking the cell first makes the text a shared string, which keeps them.
        if (cell instanceof XSSFCell xssf && xssf.getCTCell().getT() == STCellType.INLINE_STR) {
            cell.setBlank();
        }
        cell.setCellValue(rich);
    }

    /**
     * Why a theme is refused by a workbook with no room for another style. Only an {@code .xls} file has a format with
     * more room to be saved as: an {@code .xlsx} file holds 64,000 styles.
     */
    static String stylesFullMessage(Workbook workbook) {
        return workbook instanceof HSSFWorkbook
                ? "table.theme.styles.full.message"
                : "table.theme.styles.full.xlsx.message";
    }

    /** A side of a cell: where the theme names its line, and how the workbook writes it. */
    @RequiredArgsConstructor
    private enum Side {

        // In the order PoiExcelHelper.getCellBorderColors reports the colours of the sides.
        TOP(ThemeBorder::top, BorderSide.TOP, CellStyle::getBorderTop, CellStyle::setBorderTop),
        RIGHT(ThemeBorder::right, BorderSide.RIGHT, CellStyle::getBorderRight, CellStyle::setBorderRight),
        BOTTOM(ThemeBorder::bottom, BorderSide.BOTTOM, CellStyle::getBorderBottom, CellStyle::setBorderBottom),
        LEFT(ThemeBorder::left, BorderSide.LEFT, CellStyle::getBorderLeft, CellStyle::setBorderLeft);

        private final Function<ThemeBorder, @Nullable ThemeBorderLine> line;
        private final BorderSide excel;
        private final Function<CellStyle, BorderStyle> lineOf;
        private final BiConsumer<CellStyle, BorderStyle> setLine;
    }

    /** The styles and fonts one workbook was given by the theme, so each is made once. */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class WorkbookLooks {

        private final Workbook workbook;

        /** Whether the theme colours of the workbook are those the theme makes its colours of. */
        private final boolean themeColours;

        private final Map<StyleKey, CellStyle> styles = new HashMap<>();
        private final Map<FontAttributes, Font> fonts = new HashMap<>();

        /**
         * The theme colour the workbook writes a colour as: the theme colour the colour is, when the theme colours of
         * the workbook are those of the theme, or else {@code null}, for {@code #rrggbb}.
         */
        private @Nullable ThemedColor asThemeColour(ThemeColour colour) {
            return themeColours ? colour.themed() : null;
        }

        /**
         * The style a cell gets: its own with the look of the theme laid over it.
         *
         * <p>A cell that already has the look keeps its style, so writing the theme again adds no styles.
         */
        CellStyle style(CellStyle original, ThemeStyle theme) {
            return styles.computeIfAbsent(new StyleKey(original.getIndex(), theme),
                    key -> matches(original, theme) ? original : create(original, theme));
        }

        /** Whether a style already has every attribute the theme sets. */
        private boolean matches(CellStyle style, ThemeStyle theme) {
            var background = theme.background();
            var align = theme.align();
            var valign = theme.valign();
            var border = theme.border();
            return (background == null || hasFill(style, background))
                    && (align == null || style.getAlignment() == align.getExcel())
                    && (valign == null || style.getVerticalAlignment() == valign.getExcel())
                    && (border == null || hasBorder(style, border))
                    && (!theme.hasFont() || hasFont(workbook.getFontAt(style.getFontIndex()), theme));
        }

        private boolean hasFill(CellStyle style, ThemeColour background) {
            if (style.getFillPattern() != FillPatternType.SOLID_FOREGROUND) {
                return false;
            }
            var fill = style.getFillForegroundColorColor();
            var themed = asThemeColour(background);
            return themed != null
                    ? themed.equals(ThemedColor.of(fill))
                    : Arrays.equals(PoiExcelHelper.toRgb(fill), PoiExcelHelper.toStoredRgb(background.rgb(), workbook));
        }

        private boolean hasBorder(CellStyle style, ThemeBorder border) {
            var colors = PoiExcelHelper.getCellBorderColors(style, workbook);
            return Arrays.stream(Side.values())
                    .allMatch(side -> hasSide(style, side, colors[side.ordinal()], side.line.apply(border)));
        }

        /** Whether a side of a style has the line of the theme: a side without a line has no colour to match. */
        private boolean hasSide(CellStyle style, Side side, short @Nullable [] color, @Nullable ThemeBorderLine line) {
            if (line == null) {
                return true;
            }
            if (side.lineOf.apply(style) != line.style().getExcel()) {
                return false;
            }
            var themed = line.isLine() && line.color() != null ? asThemeColour(line.color()) : null;
            if (themed != null) {
                return themed.equals(ThemedColor.of(((XSSFCellStyle) style).getBorderColor(side.excel)));
            }
            return !line.isLine() || Arrays.equals(color, PoiExcelHelper.toStoredRgb(line.rgb(), workbook));
        }

        private boolean hasFont(Font font, ThemeStyle theme) {
            var attributes = FontAttributes.of(font, workbook);
            return attributes.equals(themed(attributes, theme));
        }

        /**
         * The font a piece of text gets: the given one with the font attributes of the theme laid over it. A font the
         * workbook has is reused.
         */
        Font font(Font original, ThemeStyle theme) {
            return fonts.computeIfAbsent(themed(FontAttributes.of(original, workbook), theme),
                    attributes -> PoiExcelHelper.findOrCreateFont(workbook, attributes));
        }

        /**
         * The font a look gives text written in a font of the workbook: the font with every font attribute the look
         * sets, its colour as the workbook holds it. The superscript and the character set stay those of the font.
         */
        private FontAttributes themed(FontAttributes font, ThemeStyle theme) {
            var colour = theme.color();
            return font.toBuilder()
                    .name(theme.fontFamily() != null ? theme.fontFamily() : font.name())
                    .height(theme.fontSize() != null ? (short) (theme.fontSize() * Font.TWIPS_PER_POINT)
                            : font.height())
                    .bold(theme.bold() != null ? theme.bold() : font.bold())
                    .italic(theme.italic() != null ? theme.italic() : font.italic())
                    .underline(underline(font.underline(), theme.underline()))
                    .strikeout(theme.strikeout() != null ? theme.strikeout() : font.strikeout())
                    .color(colour != null ? storedColour(colour.rgb()) : font.color())
                    .themed(colour != null ? asThemeColour(colour) : font.themed())
                    .build();
        }

        /**
         * The colour {@code #rrggbb} as one number, as the workbook holds it. Boxed, so that a font with the automatic
         * colour keeps it rather than being unboxed.
         */
        private Integer storedColour(String hex) {
            return PoiExcelHelper.toRgbValue(PoiExcelHelper.toStoredRgb(hex, workbook));
        }

        private static byte underline(byte font, @Nullable Boolean themed) {
            if (themed == null) {
                return font;
            }
            return themed ? Font.U_SINGLE : Font.U_NONE;
        }

        private CellStyle create(CellStyle original, ThemeStyle theme) {
            CellStyle style = newStyle();
            style.cloneStyleFrom(original);
            var background = theme.background();
            if (background != null) {
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                setFill(style, background);
            }
            if (theme.align() != null) {
                style.setAlignment(theme.align().getExcel());
            }
            if (theme.valign() != null) {
                style.setVerticalAlignment(theme.valign().getExcel());
            }
            if (theme.border() != null) {
                setBorder(style, theme.border());
            }
            if (theme.hasFont()) {
                style.setFont(font(workbook.getFontAt(original.getFontIndex()), theme));
            }
            return style;
        }

        /**
         * A new style of the workbook. A workbook with no room for another style refuses the theme: dropping the
         * styles it does not use, as an edit of one cell does, would renumber the styles made for the theme so far.
         */
        private CellStyle newStyle() {
            try {
                return workbook.createCellStyle();
            } catch (IllegalStateException full) {
                throw new BadRequestException(stylesFullMessage(workbook));
            }
        }

        /** Fills a style with a colour: the theme colour it is, or else {@code #rrggbb}. */
        private void setFill(CellStyle style, ThemeColour background) {
            var themed = asThemeColour(background);
            if (themed != null) {
                ((XSSFCellStyle) style).setFillForegroundColor(themed.toColor((XSSFWorkbook) workbook));
            } else {
                PoiExcelHelper.setCellFillColors(style, PoiExcelHelper.toRgb(background.rgb()), null, workbook);
            }
        }

        private void setBorder(CellStyle style, ThemeBorder border) {
            var colors = new short[Side.values().length][];
            for (var side : Side.values()) {
                var line = side.line.apply(border);
                if (line != null) {
                    side.setLine.accept(style, line.style().getExcel());
                    colors[side.ordinal()] = setThemeColour(style, side, line) ? null : colourOf(line);
                }
            }
            PoiExcelHelper.setCellBorderColors(style, colors, workbook);
        }

        /**
         * Colours a side of a style by the theme colour its line is, and tells whether the line was one: a line of
         * any other colour is coloured with the rest of the sides.
         */
        private boolean setThemeColour(CellStyle style, Side side, ThemeBorderLine line) {
            var themed = line.isLine() && line.color() != null ? asThemeColour(line.color()) : null;
            if (themed != null) {
                ((XSSFCellStyle) style).setBorderColor(side.excel, themed.toColor((XSSFWorkbook) workbook));
            }
            return themed != null;
        }

        /** The colour a side is written in; a side without a line is given none. */
        private static short @Nullable [] colourOf(ThemeBorderLine side) {
            return side.isLine() ? side.rgb() : null;
        }
    }

    /** A style the theme made, named by the style it started from and the look laid over it. */
    private record StyleKey(int original, ThemeStyle theme) {
    }
}
