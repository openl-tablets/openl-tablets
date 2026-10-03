package org.openl.studio.projects.service.tables.theme;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

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
import org.jspecify.annotations.Nullable;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STCellType;

import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.table.GridTableUtils;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.rules.table.xls.PoiExcelHelper.FontAttributes;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.service.tables.theme.ThemedTable.ThemedCell;
import org.openl.studio.projects.service.tables.write.TableWriter;

/**
 * Writes one table theme into the workbook.
 *
 * <p>Each cell keeps its own style and gets only the attributes the theme sets. Its number format, wrapping,
 * protection and every attribute the theme says nothing about stay as they are. A cell outside the table is never
 * touched. The text of a cell is never changed: the header is only formatted in pieces.
 *
 * <p>One writer serves a whole batch. A style or a font it makes once is reused by every cell that needs it. A font
 * the workbook already has is reused rather than made again, and a cell that already has the look keeps its style.
 * Writing the theme a second time therefore adds neither fonts nor styles.
 *
 * <p>A colour is compared as the workbook holds it. The palette of an {@code .xls} workbook may have no room for a
 * colour of the theme, which it then holds as the nearest colour it has: a cell holding that one has the look.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class ThemeExcelWriter {

    private final TableTheme theme;
    private final Map<Workbook, WorkbookLooks> workbooks = new IdentityHashMap<>();

    /**
     * Writes the theme into tables, notes the edit on each of them, and saves every workbook it reaches once.
     *
     * <p>A table the theme names no look for is left as it is, and so is its workbook when the theme reaches no
     * other table of it. A table the theme is written into is noted as edited, as any edit of a table is, after the
     * theme: a table without room for the note moves, so it is named by where it stands once written.
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
                if (write(table, grid)) {
                    TableWriter.recordEdit(table, edit);
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
     * <p>A table the theme names no look for is left as it is: a table of any kind but a Datatype or a Vocabulary,
     * and a table of a kind the theme leaves out.
     *
     * @param table the table to theme
     * @param grid  the table as it stands on its sheet, header included; its sheet is opened for writing
     * @return whether the theme was written into the table
     */
    public boolean write(IOpenLTable table, IGridTable grid) {
        var layout = DatatypeThemeLayout.of(table, grid, theme);
        if (layout == null) {
            return false;
        }
        var sheet = ((XlsSheetGridModel) grid.getGrid()).getSheetToWrite();
        var looks = workbooks.computeIfAbsent(sheet.getWorkbook(), WorkbookLooks::new);
        layout.cells().forEach((at, themed) -> {
            var cell = PoiExcelHelper.getOrCreateCell(at.column(), at.row(), sheet);
            var original = cell.getCellStyle();
            var style = looks.style(original, themed.style());
            // A cell that already has the look keeps its style untouched.
            if (style.getIndex() != original.getIndex()) {
                cell.setCellStyle(style);
            }
            writeRuns(cell, themed, looks);
        });
        return true;
    }

    /** Formats the header text in pieces, keeping the text as it is. */
    private static void writeRuns(Cell cell, ThemedCell themed, WorkbookLooks looks) {
        if (themed.header() == null || cell.getCellType() != CellType.STRING) {
            return;
        }
        var text = cell.getStringCellValue();
        var runs = themed.runs(text);
        if (runs.isEmpty()) {
            return;
        }
        var workbook = cell.getSheet().getWorkbook();
        var rich = workbook.getCreationHelper().createRichTextString(text);
        var cellFont = workbook.getFontAt(cell.getCellStyle().getFontIndex());
        runs.forEach(run -> rich.applyFont(run.start(), run.end(), looks.font(cellFont, run.style())));
        // A text written into the cell itself, as tools other than Excel write it, would keep its characters and
        // lose the runs. Blanking the cell first makes the text a shared string, which keeps them.
        if (cell instanceof XSSFCell xssf && xssf.getCTCell().getT() == STCellType.INLINE_STR) {
            cell.setBlank();
        }
        cell.setCellValue(rich);
    }

    /**
     * The font a look gives text written in a font of a workbook: the font with every font attribute the look sets,
     * its colour as the workbook holds it. The superscript and the character set stay those of the font.
     */
    private static FontAttributes themed(FontAttributes font, ThemeStyle theme, Workbook workbook) {
        return font.toBuilder()
                .name(theme.fontFamily() != null ? theme.fontFamily() : font.name())
                .height(theme.fontSize() != null ? (short) (theme.fontSize() * Font.TWIPS_PER_POINT) : font.height())
                .bold(theme.bold() != null ? theme.bold() : font.bold())
                .italic(theme.italic() != null ? theme.italic() : font.italic())
                .underline(underline(font.underline(), theme.underline()))
                .strikeout(theme.strikeout() != null ? theme.strikeout() : font.strikeout())
                .color(theme.color() != null ? storedColour(theme.color(), workbook) : font.color())
                .build();
    }

    /**
     * The colour {@code #rrggbb} as one number, as the workbook holds it. Boxed, so that a font with the automatic
     * colour keeps it rather than being unboxed.
     */
    private static Integer storedColour(String hex, Workbook workbook) {
        return PoiExcelHelper.toRgbValue(PoiExcelHelper.toStoredRgb(hex, workbook));
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

    private static byte underline(byte font, @Nullable Boolean themed) {
        if (themed == null) {
            return font;
        }
        return themed ? Font.U_SINGLE : Font.U_NONE;
    }

    /** The styles and fonts one workbook was given by the theme, so each is made once. */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class WorkbookLooks {

        private final Workbook workbook;
        private final Map<StyleKey, CellStyle> styles = new HashMap<>();
        private final Map<FontAttributes, Font> fonts = new HashMap<>();

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

        private boolean hasFill(CellStyle style, String background) {
            return style.getFillPattern() == FillPatternType.SOLID_FOREGROUND
                    && Arrays.equals(PoiExcelHelper.toRgb(style.getFillForegroundColorColor()),
                            PoiExcelHelper.toStoredRgb(background, workbook));
        }

        private boolean hasBorder(CellStyle style, ThemeBorder border) {
            var colors = PoiExcelHelper.getCellBorderColors(style, workbook);
            return hasSide(style.getBorderTop(), colors[0], border.top())
                    && hasSide(style.getBorderRight(), colors[1], border.right())
                    && hasSide(style.getBorderBottom(), colors[2], border.bottom())
                    && hasSide(style.getBorderLeft(), colors[3], border.left());
        }

        private boolean hasSide(BorderStyle line, short @Nullable [] color, @Nullable ThemeBorderLine side) {
            return side == null || line == side.style().getExcel()
                    && Arrays.equals(color, PoiExcelHelper.toStoredRgb(side.colorOrBlack(), workbook));
        }

        private boolean hasFont(Font font, ThemeStyle theme) {
            var attributes = FontAttributes.of(font, workbook);
            return attributes.equals(themed(attributes, theme, workbook));
        }

        /**
         * The font a piece of text gets: the given one with the font attributes of the theme laid over it. A font the
         * workbook has is reused.
         */
        Font font(Font original, ThemeStyle theme) {
            return fonts.computeIfAbsent(themed(FontAttributes.of(original, workbook), theme, workbook),
                    attributes -> PoiExcelHelper.findOrCreateFont(workbook, attributes));
        }

        private CellStyle create(CellStyle original, ThemeStyle theme) {
            CellStyle style = newStyle();
            style.cloneStyleFrom(original);
            if (theme.background() != null) {
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                PoiExcelHelper.setCellFillColors(style, PoiExcelHelper.toRgb(theme.background()), null, workbook);
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

        private void setBorder(CellStyle style, ThemeBorder border) {
            var colors = new short[4][];
            if (border.top() != null) {
                style.setBorderTop(border.top().style().getExcel());
                colors[0] = PoiExcelHelper.toRgb(border.top().colorOrBlack());
            }
            if (border.right() != null) {
                style.setBorderRight(border.right().style().getExcel());
                colors[1] = PoiExcelHelper.toRgb(border.right().colorOrBlack());
            }
            if (border.bottom() != null) {
                style.setBorderBottom(border.bottom().style().getExcel());
                colors[2] = PoiExcelHelper.toRgb(border.bottom().colorOrBlack());
            }
            if (border.left() != null) {
                style.setBorderLeft(border.left().style().getExcel());
                colors[3] = PoiExcelHelper.toRgb(border.left().colorOrBlack());
            }
            PoiExcelHelper.setCellBorderColors(style, colors, workbook);
        }
    }

    /** A style the theme made, named by the style it started from and the look laid over it. */
    private record StyleKey(int original, ThemeStyle theme) {
    }
}
