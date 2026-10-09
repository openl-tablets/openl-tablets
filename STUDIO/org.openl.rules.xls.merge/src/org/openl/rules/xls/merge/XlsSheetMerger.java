package org.openl.rules.xls.merge;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Color;
import org.apache.poi.ss.usermodel.ConditionalFormatting;
import org.apache.poi.ss.usermodel.ConditionalFormattingRule;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.ExtendedColor;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.ss.util.SheetUtil;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCol;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCols;

import org.openl.rules.xls.merge.diff.CellPart;
import org.openl.rules.xls.merge.diff.SheetMergePlan;

/**
 * Merges a sheet changed in both revisions cell by cell against the base revision.
 *
 * <p>A cell changed in one revision only takes the content of that revision. A cell changed the same way in both
 * revisions is not a conflict. The value, the style and the comment of a cell are merged each on its own, so one
 * revision may change the value and the other the style of the same cell. Row heights, column widths, the default
 * row height and column width, merged cells, pictures and the visibility of the sheet follow the same rule, each as a
 * whole.
 *
 * <p>The sheet cannot be merged, and stays a conflict, when a part of a cell or a property is changed differently in
 * both revisions, when either revision inserted or deleted rows or columns inside the content, when both revisions
 * filled empty cells next to each other, when merged cells of one revision cover a value the other revision wrote,
 * or when THEIR revision changed the data validation, the conditional formatting, the hyperlinks or the frozen panes
 * of the sheet.
 */
@Slf4j
@NullMarked
final class XlsSheetMerger {

    /**
     * Every set of cell parts by the bit mask of the parts in it, shared by all the plans.
     */
    private static final List<Set<CellPart>> PARTS = partSets();

    /**
     * The layout of a row or a column of a sheet, packed into one number.
     */
    @FunctionalInterface
    private interface Layout {
        long of(Sheet sheet, int index);
    }

    private XlsSheetMerger() {
    }

    /**
     * Plans the cell by cell merge of a sheet changed in both revisions.
     *
     * @return what the merged sheet takes from THEIR revision, or nothing if the changes conflict
     */
    static Optional<SheetMergePlan> plan(Cursor base, Cursor our, Cursor their) {
        var baseRegions = new HashSet<>(base.sheet.getMergedRegions());
        var ourRegions = new HashSet<>(our.sheet.getMergedRegions());
        var theirRegions = new HashSet<>(their.sheet.getMergedRegions());
        var mergedRegions = choose(baseRegions, ourRegions, theirRegions);
        var visibility = choose(base.isSheetHidden(), our.isSheetHidden(), their.isSheetHidden());
        var defaultLayout = choose(defaultLayout(base.sheet), defaultLayout(our.sheet), defaultLayout(their.sheet));
        var pictures = choose(base, our, their, XlsSheetsMatcher::equalDrawings);
        if (mergedRegions == Choice.CONFLICT || visibility == Choice.CONFLICT || pictures == Choice.CONFLICT
                || defaultLayout == Choice.CONFLICT
                || !settingsOf(base.sheet).equals(settingsOf(their.sheet))
                || XlsSheetShifts.isShifted(base.sheet, our.sheet, their.sheet)) {
            return Optional.empty();
        }
        var lastRow = Math.max(base.sheet.getLastRowNum(),
                Math.max(our.sheet.getLastRowNum(), their.sheet.getLastRowNum()));
        var lastColumn = Math.max(lastLaidOutColumn(base.sheet),
                Math.max(lastLaidOutColumn(our.sheet), lastLaidOutColumn(their.sheet)));
        var rows = new TreeSet<Integer>();
        var columns = new TreeSet<Integer>();
        var cells = new TreeMap<CellAddress, Set<CellPart>>();
        if (collectLayouts(base.sheet, our.sheet, their.sheet, lastRow, XlsSheetMerger::rowLayout, rows)
                && collectLayouts(base.sheet, our.sheet, their.sheet, lastColumn, XlsSheetMerger::columnLayout, columns)
                && collectCells(base, our, their, lastRow, cells)
                && !hidesValue(base, our, their, mergedRegions,
                added(mergedRegions == Choice.THEIR ? theirRegions : ourRegions, baseRegions), cells)) {
            return Optional.of(new SheetMergePlan(cells,
                    rows,
                    columns,
                    defaultLayout == Choice.THEIR,
                    mergedRegions == Choice.THEIR,
                    pictures == Choice.THEIR,
                    visibility == Choice.THEIR));
        }
        return Optional.empty();
    }

    /**
     * Applies a merge plan to OUR revision of a sheet.
     *
     * @param their THEIR revision of the sheet
     * @param our   OUR revision of the sheet, the one that is changed
     * @param plan  what the sheet takes from THEIR revision
     */
    static void apply(Cursor their, Cursor our, SheetMergePlan plan) throws IOException {
        if (plan.mergedRegions()) {
            XlsSheetCopier.replaceMergedRegions(their.sheet, our.sheet);
        }
        for (var entry : plan.cells().entrySet()) {
            copyCell(their, our, entry.getKey(), entry.getValue());
        }
        if (plan.pictures()) {
            XlsSheetCopier.copyDrawings(their, our);
        }
        for (int rowIndex : plan.rows()) {
            var theirRow = their.sheet.getRow(rowIndex);
            var ourRow = CellUtil.getRow(rowIndex, our.sheet);
            ourRow.setHeight(customHeight(theirRow));
            ourRow.setZeroHeight(theirRow != null && theirRow.getZeroHeight());
        }
        if (plan.defaultLayout()) {
            our.sheet.setDefaultColumnWidth(their.sheet.getDefaultColumnWidth());
            our.sheet.setDefaultRowHeight(their.sheet.getDefaultRowHeight());
        }
        for (int column : plan.columns()) {
            our.sheet.setColumnWidth(column, their.sheet.getColumnWidth(column));
            our.sheet.setColumnHidden(column, their.sheet.isColumnHidden(column));
        }
        if (plan.visibility()) {
            our.workbook.setSheetHidden(our.getSheetIndex(), their.isSheetHidden());
        }
    }

    /**
     * Recalculates the formulas of a workbook with merged sheets: a formula may refer to a cell taken from THEIR
     * revision, on its own sheet or on another one, and OpenL reads the value a formula cell keeps. A formula that
     * cannot be calculated keeps its value.
     */
    static void recalculate(Workbook workbook) {
        var evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        for (Sheet sheet : workbook) {
            for (Row row : sheet) {
                for (Cell cell : row) {
                    if (cell.getCellType() == CellType.FORMULA) {
                        recalculate(cell, evaluator);
                    }
                }
            }
        }
    }

    private static void recalculate(Cell cell, FormulaEvaluator evaluator) {
        try {
            evaluator.evaluateFormulaCell(cell);
        } catch (RuntimeException e) {
            log.debug("Formula of sheet={}&cell={} keeps its value: {}",
                    cell.getSheet().getSheetName(),
                    cell.getAddress(),
                    e.getMessage());
        }
    }

    /**
     * Copies parts of a cell of THEIR revision to OUR revision. A part THEIR revision has not got is cleared, and a
     * cell left with nothing in it is removed.
     */
    private static void copyCell(Cursor their, Cursor our, CellAddress address, Set<CellPart> parts) {
        their.cell = SheetUtil.getCell(their.sheet, address.getRow(), address.getColumn());
        our.row = CellUtil.getRow(address.getRow(), our.sheet);
        our.cell = CellUtil.getCell(our.row, address.getColumn());
        parts.forEach(part -> copyPart(their, our, part));
        if (XlsSheetsMatcher.isEmptyCell(our)) {
            our.row.removeCell(our.cell);
        }
        their.cell = our.cell = null;
    }

    private static void copyPart(Cursor their, Cursor our, CellPart part) {
        switch (part) {
            case VALUE -> {
                // Setting a value alone keeps the formula of a formula cell and the text of an inline string.
                our.cell.setBlank();
                if (their.cell != null) {
                    XlsSheetCopier.copyValue(their, our);
                }
            }
            case STYLE -> {
                if (their.cell == null) {
                    // No style resets the cell to the default format of the workbook, both in .xls and .xlsx.
                    our.cell.setCellStyle(null);
                } else {
                    XlsSheetCopier.copyStyles(their, our);
                }
            }
            case COMMENT -> {
                if (their.cell == null) {
                    our.cell.removeCellComment();
                } else {
                    XlsSheetCopier.copyCommentOf(their, our);
                }
            }
        }
    }

    /**
     * Collects the rows or the columns whose layout THEIR revision changed alone.
     *
     * @return {@code false} if both revisions changed the layout of a row or a column differently
     */
    private static boolean collectLayouts(Sheet base,
                                          Sheet our,
                                          Sheet their,
                                          int last,
                                          Layout layout,
                                          Set<Integer> changed) {
        for (var i = 0; i <= last; i++) {
            var choice = choose(layout.of(base, i), layout.of(our, i), layout.of(their, i));
            if (choice == Choice.CONFLICT) {
                return false;
            } else if (choice == Choice.THEIR) {
                changed.add(i);
            }
        }
        return true;
    }

    /**
     * The width of the columns and the height of the rows that have none of their own.
     */
    private static long defaultLayout(Sheet sheet) {
        return (long) sheet.getDefaultColumnWidth() << 16 | sheet.getDefaultRowHeight() & 0xFFFF;
    }

    /**
     * The height a user set for a row and whether the row is hidden.
     */
    private static long rowLayout(Sheet sheet, int rowIndex) {
        var row = sheet.getRow(rowIndex);
        return (long) customHeight(row) << 1 | (row != null && row.getZeroHeight() ? 1 : 0);
    }

    /**
     * The height a user set for a row, or {@code -1} for the default height. Excel recalculates the height of an
     * .xlsx row it fits to its content by itself, so such a height is not a change of the row.
     */
    private static short customHeight(@Nullable Row row) {
        if (row == null || row instanceof XSSFRow xssfRow && !xssfRow.getCTRow().getCustomHeight()) {
            return -1;
        }
        return row.getHeight();
    }

    /**
     * The width of a column and whether the column is hidden.
     */
    private static long columnLayout(Sheet sheet, int column) {
        return (long) sheet.getColumnWidth(column) << 1 | (sheet.isColumnHidden(column) ? 1 : 0);
    }

    /**
     * The last column that has a cell or a width or visibility of its own. An .xls sheet keeps the layout of its
     * columns in a form that cannot be listed, so all of its columns are taken.
     */
    private static int lastLaidOutColumn(Sheet sheet) {
        var last = XlsSheetShifts.lastColumn(sheet);
        if (sheet instanceof XSSFSheet xssfSheet) {
            for (CTCols cols : xssfSheet.getCTWorksheet().getColsArray()) {
                for (CTCol col : cols.getColArray()) {
                    last = Math.max(last, (int) col.getMax() - 1);
                }
            }
            return last;
        }
        return Math.max(last, SpreadsheetVersion.EXCEL97.getLastColumnIndex());
    }

    /**
     * Collects the cells THEIR revision changed alone, with the parts it changed: the value, the style and the comment
     * of a cell are merged each on its own.
     *
     * <p>Cells filled in one revision must not touch cells filled in the other one, the way changes of neighbouring
     * lines of a text conflict: OpenL takes the cells next to each other as one table, so two tables grown towards
     * each other would join.
     *
     * @return {@code false} if both revisions changed a part of a cell differently, or filled neighbouring cells
     */
    private static boolean collectCells(Cursor base,
                                        Cursor our,
                                        Cursor their,
                                        int lastRow,
                                        Map<CellAddress, Set<CellPart>> cells) {
        var ourFilled = new HashSet<CellAddress>();
        var theirFilled = new HashSet<CellAddress>();
        for (var rowIndex = 0; rowIndex <= lastRow; rowIndex++) {
            var baseRow = base.sheet.getRow(rowIndex);
            var ourRow = our.sheet.getRow(rowIndex);
            var theirRow = their.sheet.getRow(rowIndex);
            var lastColumn = Math.max(lastCellNum(baseRow), Math.max(lastCellNum(ourRow), lastCellNum(theirRow)));
            for (var column = 0; column < lastColumn; column++) {
                base.cell = cellAt(baseRow, column);
                our.cell = cellAt(ourRow, column);
                their.cell = cellAt(theirRow, column);
                var parts = theirParts(base, our, their);
                if (parts == null) {
                    return false;
                } else if (!parts.isEmpty()) {
                    cells.put(new CellAddress(rowIndex, column), parts);
                }
                if (XlsSheetsMatcher.isEmptyCell(base)) {
                    collectFilled(our, their, new CellAddress(rowIndex, column), ourFilled, theirFilled);
                }
            }
        }
        return !touch(ourFilled, theirFilled);
    }

    /**
     * Remembers which revision filled a cell empty in BASE revision.
     */
    private static void collectFilled(Cursor our,
                                      Cursor their,
                                      CellAddress address,
                                      Set<CellAddress> ourFilled,
                                      Set<CellAddress> theirFilled) {
        if (!XlsSheetsMatcher.isEmptyCell(our)) {
            ourFilled.add(address);
        }
        if (!XlsSheetsMatcher.isEmptyCell(their)) {
            theirFilled.add(address);
        }
    }

    /**
     * Tells whether a cell of one set is a neighbour of a different cell of the other set, the diagonal included.
     */
    private static boolean touch(Set<CellAddress> ourFilled, Set<CellAddress> theirFilled) {
        if (ourFilled.isEmpty()) {
            return false;
        }
        for (CellAddress address : theirFilled) {
            for (var row = address.getRow() - 1; row <= address.getRow() + 1; row++) {
                for (var column = address.getColumn() - 1; column <= address.getColumn() + 1; column++) {
                    var neighbour = new CellAddress(row, column);
                    if (!neighbour.equals(address) && ourFilled.contains(neighbour)
                            && !theirFilled.contains(neighbour)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Tells whether the merged cells the merge adds cover a value the revision that did not add them wrote: the value
     * would be hidden under the merged cell.
     */
    private static boolean hidesValue(Cursor base,
                                      Cursor our,
                                      Cursor their,
                                      Choice mergedRegions,
                                      Collection<CellRangeAddress> added,
                                      Map<CellAddress, Set<CellPart>> cells) {
        if (mergedRegions == Choice.THEIR) {
            return covers(added, address -> {
                base.cell = SheetUtil.getCell(base.sheet, address.getRow(), address.getColumn());
                our.cell = SheetUtil.getCell(our.sheet, address.getRow(), address.getColumn());
                return XlsSheetsMatcher.hasValue(our.cell) && !XlsSheetsMatcher.equalValues(base, our);
            });
        }
        return covers(added, address -> {
            var parts = cells.get(address);
            return parts != null && parts.contains(CellPart.VALUE)
                    && XlsSheetsMatcher.hasValue(SheetUtil.getCell(their.sheet, address.getRow(), address.getColumn()));
        });
    }

    private static Collection<CellRangeAddress> added(Set<CellRangeAddress> regions, Set<CellRangeAddress> base) {
        var added = new ArrayList<>(regions);
        added.removeAll(base);
        return added;
    }

    /**
     * Tells whether a cell under merged cells, other than the first one of a merged cell, holds a written value.
     */
    private static boolean covers(Collection<CellRangeAddress> regions, Predicate<CellAddress> written) {
        for (CellRangeAddress region : regions) {
            for (CellAddress address : region) {
                var covered = address.getRow() != region.getFirstRow()
                        || address.getColumn() != region.getFirstColumn();
                if (covered && written.test(address)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The parts of the current cells the merged cell takes from THEIR revision.
     *
     * @return the parts, or {@code null} if both revisions changed a part differently
     */
    private static @Nullable Set<CellPart> theirParts(Cursor base, Cursor our, Cursor their) {
        var mask = 0;
        for (CellPart part : CellPart.values()) {
            BiPredicate<Cursor, Cursor> equal = switch (part) {
                case VALUE -> XlsSheetsMatcher::equalValues;
                case STYLE -> XlsSheetsMatcher::equalStyles;
                case COMMENT -> XlsSheetsMatcher::equalComments;
            };
            var choice = choose(base, our, their, equal);
            if (choice == Choice.CONFLICT) {
                return null;
            } else if (choice == Choice.THEIR) {
                mask |= 1 << part.ordinal();
            }
        }
        return PARTS.get(mask);
    }

    private static List<Set<CellPart>> partSets() {
        var parts = CellPart.values();
        var sets = new ArrayList<Set<CellPart>>();
        for (var mask = 0; mask < 1 << parts.length; mask++) {
            var set = EnumSet.noneOf(CellPart.class);
            for (CellPart part : parts) {
                if ((mask & 1 << part.ordinal()) != 0) {
                    set.add(part);
                }
            }
            sets.add(Collections.unmodifiableSet(set));
        }
        return List.copyOf(sets);
    }

    private static int lastCellNum(@Nullable Row row) {
        return row == null ? 0 : row.getLastCellNum();
    }

    private static @Nullable Cell cellAt(@Nullable Row row, int column) {
        return row == null ? null : row.getCell(column);
    }

    /**
     * The settings of a sheet a merge cannot take from one revision: data validation, conditional formatting,
     * hyperlinks and frozen panes, as text that tells whether they changed. The order of the entries does not count.
     */
    private static List<String> settingsOf(Sheet sheet) {
        var settings = new ArrayList<String>();
        for (DataValidation validation : sheet.getDataValidations()) {
            var constraint = validation.getValidationConstraint();
            settings.add(String.join("|",
                    List.of(validation.getRegions().getCellRangeAddresses()).toString(),
                    String.valueOf(constraint.getValidationType()),
                    String.valueOf(constraint.getOperator()),
                    String.valueOf(constraint.getFormula1()),
                    String.valueOf(constraint.getFormula2()),
                    String.valueOf(constraint.getExplicitListValues() == null ? null
                            : List.of(constraint.getExplicitListValues())),
                    String.valueOf(validation.getEmptyCellAllowed()),
                    String.valueOf(validation.getSuppressDropDownArrow()),
                    String.valueOf(validation.getShowErrorBox()),
                    String.valueOf(validation.getErrorStyle()),
                    String.valueOf(validation.getErrorBoxTitle()),
                    String.valueOf(validation.getErrorBoxText()),
                    String.valueOf(validation.getShowPromptBox()),
                    String.valueOf(validation.getPromptBoxTitle()),
                    String.valueOf(validation.getPromptBoxText())));
        }
        var formatting = sheet.getSheetConditionalFormatting();
        for (var i = 0; i < formatting.getNumConditionalFormattings(); i++) {
            settings.add(describe(formatting.getConditionalFormattingAt(i)));
        }
        sheet.getHyperlinkList()
                .stream()
                .map(hyperlink -> String.join("|",
                        "link",
                        String.valueOf(hyperlink.getFirstRow()),
                        String.valueOf(hyperlink.getFirstColumn()),
                        String.valueOf(hyperlink.getType()),
                        String.valueOf(hyperlink.getAddress())))
                .forEach(settings::add);
        var pane = sheet.getPaneInformation();
        if (pane != null) {
            settings.add(String.join("|",
                    "pane",
                    String.valueOf(pane.isFreezePane()),
                    String.valueOf(pane.getHorizontalSplitPosition()),
                    String.valueOf(pane.getVerticalSplitPosition())));
        }
        settings.sort(null);
        return settings;
    }

    private static String describe(ConditionalFormatting formatting) {
        var description = new StringBuilder(List.of(formatting.getFormattingRanges()).toString());
        for (var i = 0; i < formatting.getNumberOfRules(); i++) {
            var rule = formatting.getRule(i);
            description.append('|')
                    .append(rule.getConditionType())
                    .append('|')
                    .append(rule.getComparisonOperation())
                    .append('|')
                    .append(rule.getFormula1())
                    .append('|')
                    .append(rule.getFormula2());
            describeStyle(rule, description);
        }
        return description.toString();
    }

    /**
     * Adds the fill, the font and the borders a conditional formatting rule paints the cells with.
     */
    private static void describeStyle(ConditionalFormattingRule rule, StringBuilder description) {
        Optional.ofNullable(rule.getPatternFormatting())
                .ifPresent(fill -> description.append("|fill:")
                        .append(fill.getFillPattern())
                        .append(',')
                        .append(colorOf(fill.getFillBackgroundColorColor()))
                        .append(',')
                        .append(colorOf(fill.getFillForegroundColorColor())));
        Optional.ofNullable(rule.getFontFormatting())
                .ifPresent(font -> description.append("|font:")
                        .append(font.isBold())
                        .append(',')
                        .append(font.isItalic())
                        .append(',')
                        .append(font.isStruckout())
                        .append(',')
                        .append(font.getUnderlineType())
                        .append(',')
                        .append(font.getFontHeight())
                        .append(',')
                        .append(colorOf(font.getFontColor())));
        Optional.ofNullable(rule.getBorderFormatting())
                .ifPresent(border -> description.append("|border:")
                        .append(border.getBorderTop())
                        .append(',')
                        .append(border.getBorderBottom())
                        .append(',')
                        .append(border.getBorderLeft())
                        .append(',')
                        .append(border.getBorderRight())
                        .append(',')
                        .append(colorOf(border.getTopBorderColorColor()))
                        .append(',')
                        .append(colorOf(border.getBottomBorderColorColor()))
                        .append(',')
                        .append(colorOf(border.getLeftBorderColorColor()))
                        .append(',')
                        .append(colorOf(border.getRightBorderColorColor())));
    }

    private static @Nullable String colorOf(@Nullable Color color) {
        if (color instanceof ExtendedColor extended) {
            if (extended.isIndexed()) {
                return "#" + extended.getIndex();
            }
            var base = extended.isThemed() ? "theme" + extended.getTheme() : extended.getARGBHex();
            return base + "~" + extended.getTint();
        } else if (color instanceof HSSFColor hssfColor) {
            return hssfColor.getHexString();
        }
        return null;
    }

    /**
     * Which revision a value of the merged sheet comes from.
     */
    private enum Choice {
        OUR,
        THEIR,
        CONFLICT
    }

    private static Choice choose(long base, long our, long their) {
        if (base == their) {
            return Choice.OUR;
        } else if (base == our) {
            return Choice.THEIR;
        }
        return our == their ? Choice.OUR : Choice.CONFLICT;
    }

    private static Choice choose(Object base, Object our, Object their) {
        return choose(base, our, their, Objects::equals);
    }

    /**
     * Chooses the revision of a value: THEIR revision when only it changed the value, OUR revision when the value is
     * unchanged in THEIR revision or changed the same way in both, and a conflict otherwise.
     */
    private static <T> Choice choose(T base, T our, T their, BiPredicate<T, T> equal) {
        if (equal.test(base, their)) {
            return Choice.OUR;
        } else if (equal.test(base, our)) {
            return Choice.THEIR;
        } else if (equal.test(our, their)) {
            return Choice.OUR;
        }
        return Choice.CONFLICT;
    }
}
