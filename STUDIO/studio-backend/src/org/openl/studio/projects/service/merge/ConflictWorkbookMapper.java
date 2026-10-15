package org.openl.studio.projects.service.merge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import org.openl.rules.xls.merge.XlsSheetsMatcher;
import org.openl.rules.xls.merge.XlsWorkbookMerger;
import org.openl.rules.xls.merge.diff.DiffStatus;
import org.openl.rules.xls.merge.diff.Resolution;
import org.openl.rules.xls.merge.diff.SheetConflictReason;
import org.openl.rules.xls.merge.diff.SheetConflicts;
import org.openl.rules.xls.merge.diff.TableConflict;
import org.openl.rules.xls.merge.diff.TableConflictReason;
import org.openl.rules.xls.merge.diff.WorkbookConflicts;
import org.openl.studio.projects.model.merge.ConflictSheetReason;
import org.openl.studio.projects.model.merge.ConflictSheetStatus;
import org.openl.studio.projects.model.merge.ConflictSheetView;
import org.openl.studio.projects.model.merge.ConflictTableReason;
import org.openl.studio.projects.model.merge.ConflictTableVersion;
import org.openl.studio.projects.model.merge.ConflictTableView;
import org.openl.studio.projects.model.merge.ConflictWorkbookView;
import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.model.tables.RawTableCellStyle;
import org.openl.studio.projects.model.tables.RawTableTextRun;
import org.openl.studio.projects.service.tables.read.RawTableCells;
import org.openl.studio.projects.service.tables.read.RawTableReader;

/**
 * Tells what the merge finds in a conflicted workbook the way the API reports it, with the cells of each version of a
 * conflicting table.
 */
@Component
@RequiredArgsConstructor
class ConflictWorkbookMapper {

    /**
     * The most rows of a version of a table the API reports: a screen shows a conflicting table to choose a version
     * of it, not to read all of it.
     */
    private static final int MAX_ROWS = 200;

    private final RawTableReader tableReader;

    /**
     * Describes the conflicts of a workbook, reading the cells of the conflicting tables from the revisions the merge
     * compared.
     */
    ConflictWorkbookView toView(WorkbookConflicts conflicts, XlsWorkbookMerger merger) {
        // Each sheet is prepared for reading once, for all the tables read from it.
        var readers = new IdentityHashMap<Sheet, Function<CellRangeAddress, RawTableCells>>();
        return new ConflictWorkbookView(conflicts.sheets()
                .stream()
                .map(sheet -> toSheet(sheet, merger, readers))
                .toList(), conflicts.palette());
    }

    private ConflictSheetView toSheet(SheetConflicts sheet,
                                      XlsWorkbookMerger merger,
                                      Map<Sheet, Function<CellRangeAddress, RawTableCells>> readers) {
        return new ConflictSheetView(sheet.name(),
                statusOf(sheet.status()),
                Optional.ofNullable(sheet.reason()).map(ConflictWorkbookMapper::reasonOf).orElse(null),
                sheet.inOurs(),
                sheet.inBase(),
                sheet.inTheirs(),
                sheet.tables().stream().map(table -> toTable(sheet.name(), table, merger, readers)).toList());
    }

    private static ConflictSheetStatus statusOf(DiffStatus status) {
        return switch (status) {
            case OUR -> ConflictSheetStatus.OURS;
            case THEIR -> ConflictSheetStatus.THEIRS;
            case MERGED -> ConflictSheetStatus.MERGED;
            case CONFLICT -> ConflictSheetStatus.CONFLICT;
            case BASE -> throw new IllegalStateException("A survey takes no sheet from the base version.");
        };
    }

    private static ConflictSheetReason reasonOf(SheetConflictReason reason) {
        return switch (reason) {
            case REMOVED_AND_CHANGED -> ConflictSheetReason.REMOVED_AND_CHANGED;
            case ADDED_IN_BOTH -> ConflictSheetReason.ADDED_IN_BOTH;
            case VISIBILITY -> ConflictSheetReason.VISIBILITY;
            case LAYOUT -> ConflictSheetReason.LAYOUT;
            case PICTURES -> ConflictSheetReason.PICTURES;
            case SETTINGS -> ConflictSheetReason.SETTINGS;
            case TOUCHING_TABLES -> ConflictSheetReason.TOUCHING_TABLES;
            case MOVED_CONTENT -> ConflictSheetReason.MOVED_CONTENT;
        };
    }

    private static ConflictTableReason reasonOf(TableConflictReason reason) {
        return switch (reason) {
            case CELLS -> ConflictTableReason.CELLS;
            case ROWS -> ConflictTableReason.ROWS;
            case ROWS_AND_COLUMNS -> ConflictTableReason.ROWS_AND_COLUMNS;
            case MOVED -> ConflictTableReason.MOVED;
            case REMOVED_AND_CHANGED -> ConflictTableReason.REMOVED_AND_CHANGED;
            case MERGED_CELLS -> ConflictTableReason.MERGED_CELLS;
            case ADDED_IN_BOTH -> ConflictTableReason.ADDED_IN_BOTH;
        };
    }

    private ConflictTableView toTable(String sheetName,
                                      TableConflict table,
                                      XlsWorkbookMerger merger,
                                      Map<Sheet, Function<CellRangeAddress, RawTableCells>> readers) {
        var base = read(merger, readers, Resolution.BASE, sheetName, table.base());
        var ours = read(merger, readers, Resolution.OURS, sheetName, table.ours());
        var theirs = read(merger, readers, Resolution.THEIRS, sheetName, table.theirs());
        var conflicting = conflicting(base, ours, theirs);
        return new ConflictTableView(table.id(),
                nameOf(Stream.of(base, ours, theirs).filter(Objects::nonNull).findFirst().orElseThrow()),
                reasonOf(table.reason()),
                versionOf(ours, changesOf(ours, base, theirs), conflicting),
                versionOf(base, Changes.NONE, conflicting),
                versionOf(theirs, changesOf(theirs, base, ours), conflicting));
    }

    /**
     * A table as a version of the workbook has it, read from the revision.
     *
     * @param readers the readers of the sheets read so far, which a sheet read for the first time joins
     * @param range   the place of the table, or {@code null} when the version has no such table
     * @return the table, or {@code null} when the version has no such table
     */
    private @Nullable Read read(XlsWorkbookMerger merger,
                                Map<Sheet, Function<CellRangeAddress, RawTableCells>> readers,
                                Resolution revision,
                                String sheetName,
                                @Nullable CellRangeAddress range) {
        if (range == null) {
            return null;
        }
        var sheet = merger.revisionOf(revision).getSheet(sheetName);
        var rows = range.getLastRow() - range.getFirstRow() + 1;
        var shown = rows <= MAX_ROWS
                ? range
                : new CellRangeAddress(range.getFirstRow(),
                        range.getFirstRow() + MAX_ROWS - 1,
                        range.getFirstColumn(),
                        range.getLastColumn());
        var cells = readers.computeIfAbsent(sheet, tableReader::cellsOf).apply(shown);
        var styles = Objects.requireNonNullElse(cells.styles(), List.<RawTableCellStyle>of());
        return new Read(range.formatAsString(), cells.source(), styles, rows,
                looksOf(cells.source(), styles, sheet, shown));
    }

    private static @Nullable ConflictTableVersion versionOf(@Nullable Read read,
                                                            Changes changes,
                                                            List<Position> conflicting) {
        if (read == null) {
            return null;
        }
        // A cell covered by merged cells has no address of its own: it is named by its place, so that the lists of
        // all versions name the same cells in the same order.
        var area = CellRangeAddress.valueOf(read.range());
        var conflictCells = conflicting.stream()
                .map(position -> new CellAddress(area.getFirstRow() + position.row(),
                        area.getFirstColumn() + position.column()).formatAsString())
                .toList();
        return new ConflictTableVersion(read.range(), read.cells(), read.styles(), read.rows(), changes.cells(),
                changes.formatting(), conflictCells);
    }

    /**
     * The places in a table where both versions changed what a cell holds differently, when all three versions have
     * the table at one size; none otherwise. The formatting of a cell is left out.
     */
    private static List<Position> conflicting(@Nullable Read base, @Nullable Read ours, @Nullable Read theirs) {
        if (base == null || ours == null || theirs == null || !base.sameSize(ours) || !base.sameSize(theirs)) {
            return List.of();
        }
        var conflicting = new ArrayList<Position>();
        for (var row = 0; row < base.looks().size(); row++) {
            for (var column = 0; column < base.looks().get(row).size(); column++) {
                var baseCell = base.looks().get(row).get(column);
                var ourCell = ours.looks().get(row).get(column);
                var theirCell = theirs.looks().get(row).get(column);
                if (!ourCell.sameContent(baseCell)
                        && !theirCell.sameContent(baseCell)
                        && !ourCell.sameContent(theirCell)) {
                    conflicting.add(new Position(row, column));
                }
            }
        }
        return conflicting;
    }

    /**
     * What a version of a table changed against the base version, and the other version has not alike.
     *
     * <p>A table of the size of the base one is compared cell by cell. A table with rows inserted or deleted is
     * compared by the rows that match, every cell of a row the base version has no match for being changed, and a
     * table with columns inserted or deleted the same way by columns. A row or a column matches when its cells hold
     * the same, whatever their formatting. Only the rows shown are matched, while the size counts every row of the
     * table.
     *
     * <p>A cell whose value, formula, comment or merging changed is listed. A cell that only took another formatting
     * is not: the version tells instead that its branch changed the formatting of the table.
     *
     * <p>What the other version has alike is left out: the merge takes it whichever version is taken. The other
     * version is matched the same way, so a change both branches made, such as the formatting a save gives every
     * table, is left out even when one of them inserted or deleted rows.
     *
     * <p>A table that differs from the base one in both the number of rows and the number of columns tells nothing,
     * and so does a table wider than {@value #MAX_ROWS} columns with columns inserted or deleted.
     *
     * @param other the table in the other version, or {@code null} when it has none
     */
    private static Changes changesOf(@Nullable Read version, @Nullable Read base, @Nullable Read other) {
        if (version == null || base == null) {
            return Changes.NONE;
        }
        var inBase = counterparts(version, base);
        if (inBase == null) {
            return Changes.NONE;
        }
        var inOther = other == null ? null : counterparts(version, other);
        var cells = new ArrayList<String>();
        var formatting = false;
        for (var row = 0; row < version.cells().size(); row++) {
            for (var column = 0; column < version.cells().get(row).size(); column++) {
                var look = version.looks().get(row).get(column);
                var baseLook = inBase.at(row, column);
                var otherLook = inOther == null ? null : inOther.at(row, column);
                if (baseLook == null || !look.sameContent(baseLook)) {
                    var cell = version.cells().get(row).get(column).cell();
                    if (cell != null && (otherLook == null || !look.sameContent(otherLook))) {
                        cells.add(cell);
                    }
                } else if (!look.formattedAs(baseLook) && !look.formattedAs(otherLook)) {
                    formatting = true;
                }
            }
        }
        return new Changes(cells, formatting);
    }

    /**
     * Finds the cells of a version of a table in another version: at the same places when both have the table at one
     * size, in the rows or the columns that match otherwise.
     *
     * @return the cells, or {@code null} when the versions differ in both the number of rows and the number of
     * columns, or have columns inserted or deleted in a table wider than {@value #MAX_ROWS} columns
     */
    private static @Nullable Counterparts counterparts(Read version, Read other) {
        var cells = version.looks();
        var otherCells = other.looks();
        if (version.sameSize(other)) {
            return (row, column) -> otherCells.get(row).get(column);
        } else if (widthOf(cells) == widthOf(otherCells)) {
            var rows = matchedLines(cells.size(), otherCells.size(),
                    (row, otherRow) -> sameRow(cells.get(row), otherCells.get(otherRow)));
            return (row, column) -> rows[row] < 0 ? null : otherCells.get(rows[row]).get(column);
        } else if (version.rows() == other.rows() && Math.max(widthOf(cells), widthOf(otherCells)) <= MAX_ROWS) {
            var columns = matchedLines(widthOf(cells), widthOf(otherCells),
                    (column, otherColumn) -> sameColumn(cells, column, otherCells, otherColumn));
            return (row, column) -> columns[column] < 0 ? null : otherCells.get(row).get(columns[column]);
        }
        return null;
    }

    /** The cells of another version of a table that the cells of a version stand for. */
    @FunctionalInterface
    private interface Counterparts {

        /** The cell of the other version at the place of a cell of the version, or {@code null} when it has none. */
        @Nullable Look at(int row, int column);
    }

    /**
     * What a version of a table changed: the cells whose value, formula, comment or merging changed, and whether the
     * formatting of other cells changed.
     */
    private record Changes(List<String> cells, boolean formatting) {

        static final Changes NONE = new Changes(List.of(), false);
    }

    /**
     * Finds the lines of a version in another version, a line being a row or a column of a table. The matches are the
     * most lines both versions have alike in the same order; the other lines were inserted or changed.
     *
     * @param same tells whether a line of the version, the first argument, holds the same as a line of the other
     *             version
     * @return for each line of the version, the line of the other version it matches, or {@code -1} for none
     */
    private static int[] matchedLines(int count, int otherCount, BiPredicate<Integer, Integer> same) {
        // common[line][otherLine] is how many lines match from these lines to the ends of both versions.
        var common = new int[count + 1][otherCount + 1];
        for (var line = count - 1; line >= 0; line--) {
            for (var otherLine = otherCount - 1; otherLine >= 0; otherLine--) {
                common[line][otherLine] = same.test(line, otherLine)
                        ? common[line + 1][otherLine + 1] + 1
                        : Math.max(common[line + 1][otherLine], common[line][otherLine + 1]);
            }
        }
        var matched = new int[count];
        Arrays.fill(matched, -1);
        var line = 0;
        var otherLine = 0;
        while (line < count && otherLine < otherCount) {
            if (same.test(line, otherLine)) {
                matched[line] = otherLine;
                line++;
                otherLine++;
            } else if (common[line + 1][otherLine] >= common[line][otherLine + 1]) {
                line++;
            } else {
                otherLine++;
            }
        }
        return matched;
    }

    private static int widthOf(List<List<Look>> cells) {
        return cells.isEmpty() ? 0 : cells.getFirst().size();
    }

    private static boolean sameRow(List<Look> row, List<Look> otherRow) {
        for (var column = 0; column < row.size(); column++) {
            if (!row.get(column).holdsAs(otherRow.get(column))) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameColumn(List<List<Look>> cells,
                                      int column,
                                      List<List<Look>> otherCells,
                                      int otherColumn) {
        for (var row = 0; row < cells.size(); row++) {
            if (!cells.get(row).get(column).holdsAs(otherCells.get(row).get(otherColumn))) {
                return false;
            }
        }
        return true;
    }

    /**
     * What the cells of a version show, each style named by index replaced with the style itself, so that the cells of
     * two reads compare.
     *
     * @param sheet the sheet the cells are read from
     * @param area  the area of the sheet the cells are read from
     */
    private static List<List<Look>> looksOf(List<List<RawTableCell>> cells,
                                            List<RawTableCellStyle> styles,
                                            Sheet sheet,
                                            CellRangeAddress area) {
        var looks = new ArrayList<List<Look>>();
        for (var row = 0; row < cells.size(); row++) {
            var sheetRow = sheet.getRow(area.getFirstRow() + row);
            var rowLooks = new ArrayList<Look>();
            for (var column = 0; column < cells.get(row).size(); column++) {
                var source = sheetRow == null ? null : sheetRow.getCell(area.getFirstColumn() + column);
                rowLooks.add(Look.of(cells.get(row).get(column), styles, source));
            }
            looks.add(rowLooks);
        }
        return looks;
    }

    /**
     * What a cell shows: the value, the formula, the comment, the fonts of pieces of its text and the merging, with
     * the cell of the sheet to compare its formatting by.
     */
    private record Look(@Nullable Object value,
                        @Nullable String formula,
                        @Nullable String comment,
                        List<Run> runs,
                        @Nullable Integer colspan,
                        @Nullable Integer rowspan,
                        @Nullable Boolean covered,
                        @Nullable Cell source) {

        static Look of(RawTableCell cell, List<RawTableCellStyle> styles, @Nullable Cell source) {
            var runs = Objects.requireNonNullElse(cell.runs(), List.<RawTableTextRun>of())
                    .stream()
                    .map(run -> new Run(run.to(), styleAt(styles, run.style())))
                    .toList();
            return new Look(cell.value(),
                    cell.formula(),
                    cell.comment(),
                    runs,
                    cell.colspan(),
                    cell.rowspan(),
                    cell.covered(),
                    source);
        }

        /**
         * Tells whether two cells are formatted alike, as the merge compares them: the font is compared by its name
         * and size too, which the styles of the API leave out. No cell of the other version, {@code null}, is formatted
         * unlike any cell.
         */
        boolean formattedAs(@Nullable Look other) {
            return other != null && runs.equals(other.runs) && XlsSheetsMatcher.equalFormatting(source, other.source);
        }

        /**
         * Tells whether two cells hold the same value, formula and comment, whatever their formatting and merging.
         */
        boolean holdsAs(Look other) {
            return Objects.equals(value, other.value)
                    && Objects.equals(formula, other.formula)
                    && Objects.equals(comment, other.comment);
        }

        /**
         * Tells whether two cells hold the same and are merged alike, whatever their formatting.
         */
        boolean sameContent(Look other) {
            return holdsAs(other)
                    && Objects.equals(colspan, other.colspan)
                    && Objects.equals(rowspan, other.rowspan)
                    && Objects.equals(covered, other.covered);
        }
    }

    /**
     * A table as a version has it: its place, its cells as read, the styles they name, how many rows it has, and what
     * each cell shows.
     */
    private record Read(String range,
                        List<List<RawTableCell>> cells,
                        List<RawTableCellStyle> styles,
                        int rows,
                        List<List<Look>> looks) {

        /** Whether two versions have the table at one size: the same number of rows, and of columns. */
        boolean sameSize(Read other) {
            return rows == other.rows && widthOf(looks) == widthOf(other.looks);
        }
    }

    /** The place of a cell in a table, from its top left cell. */
    private record Position(int row, int column) {
    }

    /** A piece of the text of a cell with a font of its own, ending at an index of the text. */
    private record Run(int to, @Nullable RawTableCellStyle style) {
    }

    private static @Nullable RawTableCellStyle styleAt(List<RawTableCellStyle> styles, @Nullable Integer index) {
        return index == null || index < 0 || index >= styles.size() ? null : styles.get(index);
    }

    /**
     * The text of the first cell of a table, which heads an OpenL table, or the place of the table when the cell has
     * no text.
     */
    private static String nameOf(Read version) {
        return version.cells()
                .stream()
                .findFirst()
                .flatMap(row -> row.stream().findFirst())
                .map(RawTableCell::value)
                .map(String::valueOf)
                .filter(text -> !text.isBlank())
                .orElse(version.range());
    }
}
