package org.openl.rules.xls.merge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.poi.hssf.usermodel.HSSFComment;
import org.apache.poi.hssf.usermodel.HSSFPatriarch;
import org.apache.poi.hssf.usermodel.HSSFShape;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.SheetUtil;
import org.apache.poi.xssf.usermodel.XSSFShape;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import org.openl.rules.xls.merge.XlsSheetMerger.Choice;
import org.openl.rules.xls.merge.diff.CellPart;
import org.openl.rules.xls.merge.diff.CellSource;

/**
 * Merges a sheet changed in both revisions table by table against the base revision.
 *
 * <p>Each revision of the sheet is split into tables, and the tables of BASE revision are found in the other
 * revisions. A table added, removed, changed or moved in one revision only takes the change of that revision. A table
 * changed in both revisions is merged line by line, the way lines of text are merged: its rows, or its columns, are
 * aligned with BASE revision, and the lines inserted, deleted or changed in one revision only are taken from it. The
 * cells of a line changed in both revisions are merged part by part.
 *
 * <p>The sheet stays a conflict when both revisions change the same lines differently, insert different lines at the
 * same place, move a table to different places, or delete a table the other revision changes. It stays a conflict as
 * well when one revision changes the rows and the other the columns of one table, when a change crosses merged cells,
 * when merged tables would touch each other, and when moved content is referred to by a formula, a defined name, a
 * data validation, a conditional formatting or a hyperlink.
 */
@NullMarked
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
final class XlsTableMerger {

    private static final Set<CellPart> ALL_PARTS = Set.copyOf(EnumSet.allOf(CellPart.class));
    private static final CellSource CLEARED = new CellSource(null, null, Set.of());
    /**
     * The revisions whose lines a merged table takes.
     */
    private static final List<Side> REVISIONS = List.of(Side.OUR, Side.THEIR);

    private final Cursor base;
    private final Cursor our;
    private final Cursor their;
    /**
     * Tells whether THEIR revision changed or created a sheet, by its name.
     */
    private final Predicate<String> changedInTheirs;

    /**
     * Tables of OUR revision whose cells are cleared before the merged tables are written.
     */
    private final List<CellRangeAddress> clearedTables = new ArrayList<>();
    /**
     * Merged tables written into OUR revision.
     */
    private final List<TableOutput> outputs = new ArrayList<>();
    /**
     * The places of all the tables of the merged sheet.
     */
    private final List<CellRangeAddress> layout = new ArrayList<>();
    /**
     * Areas of OUR revision whose content moves to other addresses.
     */
    private final List<CellRangeAddress> ourMoved = new ArrayList<>();
    /**
     * Areas of THEIR revision whose content lands at other addresses.
     */
    private final List<CellRangeAddress> theirMoved = new ArrayList<>();

    /**
     * The cells and the merged cells of a sheet merged table by table.
     *
     * @param cells         cells of OUR revision that change, with the content they take
     * @param mergedRegions all the merged cells of the merged sheet, or {@code null} to keep those of OUR revision
     * @param moved         areas whose content lands at other addresses than a revision has it at
     */
    record Content(Map<CellAddress, CellSource> cells,
                   @Nullable List<CellRangeAddress> mergedRegions,
                   List<CellRangeAddress> moved) {
    }

    /**
     * Plans the table by table merge of a sheet changed in both revisions.
     *
     * @param changedInTheirs tells whether THEIR revision changed or created a sheet, by its name
     * @return what changes in OUR revision of the sheet, or nothing if the changes conflict
     */
    static Optional<Content> plan(Cursor base, Cursor our, Cursor their, Predicate<String> changedInTheirs) {
        var merger = new XlsTableMerger(base, our, their, changedInTheirs);
        return merger.merge() ? Optional.of(merger.content()) : Optional.empty();
    }

    private boolean merge() {
        var baseTables = XlsSheetTables.split(base.sheet);
        var ourTables = XlsSheetTables.split(our.sheet);
        var theirTables = XlsSheetTables.split(their.sheet);
        var ourPairs = XlsSheetTables.match(baseTables, base.sheet, ourTables, our.sheet);
        var theirPairs = XlsSheetTables.match(baseTables, base.sheet, theirTables, their.sheet);
        for (CellRangeAddress baseTable : baseTables) {
            if (!mergeTable(baseTable, ourPairs.get(baseTable), theirPairs.get(baseTable))) {
                return false;
            }
        }
        var ourCreated = new ArrayList<>(ourTables);
        ourCreated.removeAll(new HashSet<>(ourPairs.values()));
        var theirCreated = new ArrayList<>(theirTables);
        theirCreated.removeAll(new HashSet<>(theirPairs.values()));
        for (CellRangeAddress table : theirCreated) {
            var same = ourCreated.contains(table) && equalTables(view(our, table), view(their, table));
            if (!same && !writeWhole(null, Side.THEIR, view(their, table), null, table)) {
                return false;
            }
        }
        layout.addAll(ourCreated);
        return isSeparated() && !refersToMoved();
    }

    /**
     * Clears the tables of OUR revision that change first, and then writes the merged tables, which may take their
     * places.
     */
    private Content content() {
        var cells = new LinkedHashMap<CellAddress, CellSource>();
        for (CellRangeAddress table : clearedTables) {
            for (CellAddress address : table) {
                cells.put(address, CLEARED);
            }
        }
        for (TableOutput output : outputs) {
            output.cells.forEach((address, source) -> {
                if (address.equals(source.our()) && source.parts().isEmpty()) {
                    // the cell keeps its place and its content
                    cells.remove(address);
                } else {
                    cells.put(address, source);
                }
            });
        }
        var moved = new ArrayList<CellRangeAddress>(ourMoved);
        moved.addAll(theirMoved);
        if (clearedTables.isEmpty() && outputs.isEmpty()) {
            return new Content(cells, null, moved);
        }
        var regions = our.getMergedRegions()
                .stream()
                .filter(region -> clearedTables.stream().noneMatch(table -> table.intersects(region)))
                .collect(Collectors.toCollection(ArrayList::new));
        outputs.forEach(output -> regions.addAll(output.regions));
        return new Content(cells, regions, moved);
    }

    /**
     * Merges one table of BASE revision with its revisions.
     *
     * @return {@code false} if the changes of the table conflict
     */
    private boolean mergeTable(CellRangeAddress baseTable,
                               @Nullable CellRangeAddress ourTable,
                               @Nullable CellRangeAddress theirTable) {
        var baseView = view(base, baseTable);
        if (ourTable == null || theirTable == null) {
            return mergeRemoved(baseView, ourTable, theirTable);
        }
        var ourView = view(our, ourTable);
        var theirView = view(their, theirTable);
        var place = XlsSheetMerger.choose(start(baseTable), start(ourTable), start(theirTable), Object::equals);
        if (place == Choice.CONFLICT) {
            return false;
        }
        var start = start(place == Choice.THEIR ? theirTable : ourTable);
        if (equalTables(baseView, theirView) || equalTables(ourView, theirView)) {
            if (place == Choice.OUR) {
                layout.add(ourTable);
                otherMoved(Side.OUR, theirTable, ourTable);
                return true;
            }
            return writeWhole(ourTable, Side.OUR, ourView, theirTable, place(start, ourTable));
        } else if (equalTables(baseView, ourView)) {
            return writeWhole(ourTable, Side.THEIR, theirView, ourTable, place(start, theirTable));
        }
        return mergeLines(baseTable, ourTable, theirTable, start);
    }

    /**
     * Merges a table that one or both revisions removed: a removed table stays removed when the other revision left
     * its content as it was, and conflicts otherwise.
     */
    private boolean mergeRemoved(View baseView,
                                 @Nullable CellRangeAddress ourTable,
                                 @Nullable CellRangeAddress theirTable) {
        if (ourTable != null) {
            if (!equalTables(baseView, view(our, ourTable))) {
                return false;
            }
            clearedTables.add(ourTable);
            return true;
        }
        return theirTable == null || equalTables(baseView, view(their, theirTable));
    }

    /**
     * Merges a table changed in both revisions line by line: by rows, or by columns when a revision inserted or
     * deleted columns. Only one of the two can be merged at a time.
     */
    private boolean mergeLines(CellRangeAddress baseTable,
                               CellRangeAddress ourTable,
                               CellRangeAddress theirTable,
                               CellAddress start) {
        var rows = new Views(view(base, baseTable), view(our, ourTable), view(their, theirTable));
        var columns = new Views(new View(base, baseTable, true),
                new View(our, ourTable, true),
                new View(their, theirTable, true));
        var byRows = alignments(rows);
        var byColumns = alignments(columns);
        if (byRows == null || byColumns == null || byRows.isStructural() && byColumns.isStructural()) {
            return false;
        }
        var transposed = byColumns.isStructural();
        var alignments = transposed ? byColumns : byRows;
        var views = transposed ? columns : rows;
        var positions = views.base().positions();
        if (positions != views.our().positions() || positions != views.their().positions()) {
            return false;
        }
        var lines = new LineMerger(views.base(), views.our(), views.their(), alignments).merge();
        if (lines == null) {
            return false;
        }
        var target = transposed
                ? place(start, positions, lines.size())
                : place(start, lines.size(), positions);
        return write(ourTable, views, new Lines(lines, alignments), target);
    }

    /**
     * Aligns the lines of BASE revision of a table with OUR and THEIR revisions.
     *
     * @return the alignments, or {@code null} when too many lines changed to align them
     */
    private static @Nullable Alignments alignments(Views views) {
        var base = views.base().cellDigests();
        var baseLines = lineDigests(base);
        var ourAlignment = align(base, baseLines, views.our().cellDigests());
        var theirAlignment = align(base, baseLines, views.their().cellDigests());
        return ourAlignment == null || theirAlignment == null ? null : new Alignments(ourAlignment, theirAlignment);
    }

    private static @Nullable LineAlignment align(long[][] base, long[] baseLines, long[][] side) {
        return LineAlignment.align(baseLines, lineDigests(side), similarity(base, side));
    }

    /**
     * Digests of lines from the digests of their cells, which tell lines apart.
     */
    private static long[] lineDigests(long[][] cells) {
        var digests = new long[cells.length];
        for (var line = 0; line < cells.length; line++) {
            for (var position = 0; position < cells[line].length; position++) {
                digests[line] += XlsSheetShifts.mix(position, cells[line][position]);
            }
        }
        return digests;
    }

    /**
     * Tells how alike the lines of two tables are: the number of values they share at the same positions, when they
     * share at least half of their values.
     */
    private static LineAlignment.Similarity similarity(long[][] cells, long[][] otherCells) {
        return (line, otherLine) -> {
            var shared = 0;
            var filled = 0;
            var otherFilled = 0;
            for (var position = 0; position < Math.min(cells[line].length, otherCells[otherLine].length); position++) {
                var digest = cells[line][position];
                var otherDigest = otherCells[otherLine][position];
                filled += digest == 0 ? 0 : 1;
                otherFilled += otherDigest == 0 ? 0 : 1;
                shared += digest != 0 && digest == otherDigest ? 1 : 0;
            }
            return shared > 0 && 2 * shared >= Math.max(filled, otherFilled) ? shared : -1;
        };
    }

    /**
     * Writes a table taken whole from one revision. The other revision has the table as BASE revision has it, so its
     * content moves when the table changes its size or its place.
     *
     * @param other the table in the other revision, or {@code null} for a table the source revision created
     */
    private boolean writeWhole(@Nullable CellRangeAddress ourTable,
                               Side side,
                               View source,
                               @Nullable CellRangeAddress other,
                               CellRangeAddress target) {
        var views = side == Side.OUR ? new Views(null, source, null) : new Views(null, null, source);
        if (!write(ourTable, views, Lines.whole(side, source.lines()), target)) {
            return false;
        }
        otherMoved(side, other, target);
        return true;
    }

    /**
     * Records the move of a table the merge takes from one revision: the other revision has the table as BASE
     * revision has it, so its content moves when the table lands at another place or takes another size.
     *
     * @param source the revision the table is taken from
     * @param other  the table in the other revision, or {@code null} when it has none
     */
    private void otherMoved(Side source, @Nullable CellRangeAddress other, CellRangeAddress target) {
        if (other != null && !other.equals(target)) {
            var moved = source == Side.OUR ? theirMoved : ourMoved;
            moved.add(other);
            moved.add(target);
        }
    }

    /**
     * Writes a merged table at its place, clearing the place of the table in OUR revision first.
     *
     * @return {@code false} if a change crosses the merged cells of the table
     */
    private boolean write(@Nullable CellRangeAddress ourTable, Views views, Lines lines, CellRangeAddress target) {
        var output = new TableOutput(views, lines, target);
        if (!output.build()) {
            return false;
        }
        if (ourTable != null) {
            clearedTables.add(ourTable);
        }
        layout.add(target);
        outputs.add(output);
        ourMoved.addAll(output.moved(Side.OUR));
        theirMoved.addAll(output.moved(Side.THEIR));
        return true;
    }

    /**
     * Tells whether every two tables of the merged sheet have an empty row or column between them, so OpenL does not
     * read them as one table.
     */
    private boolean isSeparated() {
        for (var i = 0; i < layout.size(); i++) {
            for (var j = i + 1; j < layout.size(); j++) {
                if (XlsSheetTables.touch(layout.get(i), layout.get(j))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Tells whether something the merged sheet keeps at its address refers to content that moves.
     *
     * <p>The merged sheet keeps the data validation, the conditional formatting, the hyperlinks and the defined names
     * of OUR revision, and THEIR revision has them as in BASE revision. It keeps the formulas of OUR revision, and a
     * formula of THEIR revision when it takes the value of the cell from THEIR revision, or takes a whole sheet THEIR
     * revision alone changed.
     */
    private boolean refersToMoved() {
        if (ourRefersToMoved() || objectsOver(their, theirMoved)) {
            return true;
        }
        var taken = outputs.stream()
                .flatMap(output -> output.cells.values().stream())
                .filter(source -> source.parts().contains(CellPart.VALUE))
                .map(CellSource::their)
                .collect(Collectors.toSet());
        Predicate<Cell> kept = cell -> cell.getSheet().getSheetName().equals(their.sheet.getSheetName())
                ? taken.contains(cell.getAddress())
                : changedInTheirs.test(cell.getSheet().getSheetName());
        return XlsFormulaReferences.refersTo(their.originalWorkbook(), their.sheet.getSheetName(), theirMoved, kept);
    }

    /**
     * Tells whether something laid over the cells of a revision lies over its content that moves: a picture, a
     * chart or another shape, an Excel table or an autofilter. Such an object keeps its cells, so it would stay
     * behind the content it belongs to.
     */
    private static boolean objectsOver(Cursor side, List<CellRangeAddress> moved) {
        if (moved.isEmpty()) {
            return false;
        }
        var areas = new ArrayList<CellRangeAddress>();
        if (side.sheet instanceof XSSFSheet sheet) {
            addObjectAreas(side, sheet, areas);
        } else if (side.sheet.getDrawingPatriarch() instanceof HSSFPatriarch patriarch) {
            for (HSSFShape shape : patriarch.getChildren()) {
                // a comment moves with its cell
                if (!(shape instanceof HSSFComment) && shape.getAnchor() instanceof ClientAnchor anchor) {
                    areas.add(XlsSheetCopier.areaOf(anchor));
                }
            }
        }
        return areas.stream().anyMatch(area -> moved.stream().anyMatch(area::intersects));
    }

    /**
     * Adds the areas of the shapes, the Excel tables and the autofilter of an .xlsx sheet.
     */
    private static void addObjectAreas(Cursor side, XSSFSheet sheet, List<CellRangeAddress> areas) {
        for (XSSFShape shape : side.getSheetShapes()) {
            if (shape.getAnchor() instanceof ClientAnchor anchor) {
                areas.add(XlsSheetCopier.areaOf(anchor));
            }
        }
        for (XSSFTable table : sheet.getTables()) {
            areas.add(new CellRangeAddress(table.getStartRowIndex(),
                    table.getEndRowIndex(),
                    table.getStartColIndex(),
                    table.getEndColIndex()));
        }
        if (sheet.getCTWorksheet().isSetAutoFilter()) {
            areas.add(CellRangeAddress.valueOf(sheet.getCTWorksheet().getAutoFilter().getRef()));
        }
    }


    /**
     * Tells whether something of OUR revision that keeps an address refers to its content that moves: an object over
     * it, a formula, a defined name, a data validation, a conditional formatting or a hyperlink.
     */
    private boolean ourRefersToMoved() {
        return objectsOver(our, ourMoved)
                || XlsFormulaReferences.anythingRefersTo(our.originalWorkbook(), our.sheet, ourMoved);
    }

    /**
     * Tells whether two tables have the same size, cells and merged cells.
     */
    private static boolean equalTables(View a, View b) {
        if (a.lines() != b.lines() || a.positions() != b.positions()) {
            return false;
        }
        for (var line = 0; line < a.lines(); line++) {
            if (!a.equalLine(line, b, line)) {
                return false;
            }
        }
        return a.regions().equals(b.regions());
    }

    private static CellAddress start(CellRangeAddress table) {
        return new CellAddress(table.getFirstRow(), table.getFirstColumn());
    }

    private static CellRangeAddress place(CellAddress start, CellRangeAddress table) {
        return place(start, table.getLastRow() - table.getFirstRow() + 1,
                table.getLastColumn() - table.getFirstColumn() + 1);
    }

    private static CellRangeAddress place(CellAddress start, int rows, int columns) {
        return new CellRangeAddress(start.getRow(),
                start.getRow() + rows - 1,
                start.getColumn(),
                start.getColumn() + columns - 1);
    }

    private static CellRangeAddress area(CellAddress first, CellAddress last) {
        return new CellRangeAddress(first.getRow(), last.getRow(), first.getColumn(), last.getColumn());
    }

    /**
     * The address of a cell of a table seen as lines from its first cell.
     */
    private static CellAddress address(CellRangeAddress table, boolean transposed, int line, int position) {
        return transposed
                ? new CellAddress(table.getFirstRow() + position, table.getFirstColumn() + line)
                : new CellAddress(table.getFirstRow() + line, table.getFirstColumn() + position);
    }

    /**
     * A table of a revision seen by its rows.
     */
    private static View view(Cursor cursor, CellRangeAddress table) {
        return new View(cursor, table, false);
    }

    /**
     * A revision of the merge.
     */
    private enum Side {
        BASE,
        OUR,
        THEIR
    }

    /**
     * A table of a revision seen as lines: its rows, or its columns when it is transposed. A position is the place of a
     * cell along its line.
     */
    private record View(Cursor cursor, CellRangeAddress table, boolean transposed) {

        int lines() {
            return transposed ? columns() : rows();
        }

        int positions() {
            return transposed ? rows() : columns();
        }

        private int rows() {
            return table.getLastRow() - table.getFirstRow() + 1;
        }

        private int columns() {
            return table.getLastColumn() - table.getFirstColumn() + 1;
        }

        CellAddress address(int line, int position) {
            return XlsTableMerger.address(table, transposed, line, position);
        }

        @Nullable Cell cell(int line, int position) {
            var address = address(line, position);
            return SheetUtil.getCell(cursor.sheet, address.getRow(), address.getColumn());
        }

        /**
         * Digests of the values of the cells by line and position, {@code 0} for a cell with nothing in it.
         */
        long[][] cellDigests() {
            var digests = new long[lines()][positions()];
            for (var line = 0; line < digests.length; line++) {
                for (var position = 0; position < positions(); position++) {
                    var cell = cell(line, position);
                    digests[line][position] = cell == null ? 0 : XlsSheetShifts.digest(cell);
                }
            }
            return digests;
        }

        /**
         * Tells whether a line of this table and a line of another table have the same cells: values, styles and
         * comments.
         */
        boolean equalLine(int line, View other, int otherLine) {
            for (var position = 0; position < positions(); position++) {
                cursor.cell = cell(line, position);
                other.cursor.cell = other.cell(otherLine, position);
                var equal = XlsSheetsMatcher.equalValues(cursor, other.cursor)
                        && XlsSheetsMatcher.equalStyles(cursor, other.cursor)
                        && XlsSheetsMatcher.equalComments(cursor, other.cursor);
                cursor.cell = other.cursor.cell = null;
                if (!equal) {
                    return false;
                }
            }
            return true;
        }

        /**
         * The merged cells inside the table, by the lines and the positions they cover.
         */
        Set<Span> regions() {
            return cursor.getMergedRegions()
                    .stream()
                    .filter(table::intersects)
                    .map(this::span)
                    .collect(Collectors.toSet());
        }

        private Span span(CellRangeAddress region) {
            var rows = new Span(region.getFirstRow() - table.getFirstRow(),
                    region.getLastRow() - table.getFirstRow(),
                    region.getFirstColumn() - table.getFirstColumn(),
                    region.getLastColumn() - table.getFirstColumn());
            return transposed ? new Span(rows.firstPosition, rows.lastPosition, rows.firstLine, rows.lastLine) : rows;
        }
    }

    /**
     * Merged cells of a table by the lines and the positions they cover.
     */
    private record Span(int firstLine, int lastLine, int firstPosition, int lastPosition) {
    }

    /**
     * Merged cells of a revision of a table by the lines of BASE revision they cover.
     *
     * @param firstLine the first line of BASE revision the merged cells cover
     * @param lastLine  the last line of BASE revision the merged cells cover
     * @param length    the number of lines the merged cells cover in the revision, inserted lines included
     */
    private record RegionKey(int firstLine, int lastLine, int firstPosition, int lastPosition, int length) {
    }

    /**
     * The tables of a merge seen the same way. A revision that gives no lines to the merged table has no view.
     */
    private record Views(@Nullable View base, @Nullable View our, @Nullable View their) {

        /**
         * The view of OUR revision, or of THEIR revision when OUR revision gives no lines.
         */
        View any() {
            return Objects.requireNonNull(our != null ? our : their);
        }

        @Nullable View of(Side side) {
            return switch (side) {
                case BASE -> base;
                case OUR -> our;
                case THEIR -> their;
            };
        }
    }

    /**
     * The alignments of the lines of a table of BASE revision with OUR and THEIR revisions.
     */
    private record Alignments(LineAlignment our, LineAlignment their) {

        boolean isStructural() {
            return our.isStructural() || their.isStructural();
        }

        LineAlignment of(Side side) {
            return side == Side.OUR ? our : their;
        }
    }

    /**
     * A line of a merged table: the lines of the revisions it comes from, {@code -1} for a revision it is not in.
     */
    private record Line(int base, int our, int their) {

        /**
         * A line only one revision has.
         */
        static Line of(Side side, int line) {
            return side == Side.OUR ? new Line(-1, line, -1) : new Line(-1, -1, line);
        }

        int of(Side side) {
            return switch (side) {
                case BASE -> base;
                case OUR -> our;
                case THEIR -> their;
            };
        }
    }

    /**
     * The lines of a merged table, and how the revisions were aligned to get them.
     *
     * @param alignments the alignments of the lines, or {@code null} when the table is taken whole from one revision
     */
    private record Lines(List<Line> lines, @Nullable Alignments alignments) {

        /**
         * The rows of a table taken whole from one revision.
         */
        static Lines whole(Side side, int count) {
            var lines = new ArrayList<Line>(count);
            for (var i = 0; i < count; i++) {
                lines.add(Line.of(side, i));
            }
            return new Lines(lines, null);
        }
    }

    /**
     * Merges the lines of a table changed in both revisions, the way a three-way merge of text merges lines.
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class LineMerger {

        private final View base;
        private final View our;
        private final View their;
        private final Alignments alignments;

        /**
         * @return the lines of the merged table, or {@code null} if the changes conflict
         */
        @Nullable List<Line> merge() {
            var ourHunks = alignments.our().hunks();
            var theirHunks = new ArrayList<LineAlignment.Hunk>();
            for (LineAlignment.Hunk hunk : alignments.their().hunks()) {
                var same = ourHunks.stream().anyMatch(ourHunk -> sameHunk(ourHunk, hunk));
                if (!same) {
                    if (ourHunks.stream().anyMatch(ourHunk -> overlap(ourHunk, hunk))) {
                        return null;
                    }
                    theirHunks.add(hunk);
                }
            }
            if (changedUnder(ourHunks, their, alignments.their()) || changedUnder(theirHunks, our, alignments.our())) {
                return null;
            }
            return lines(ourHunks, theirHunks);
        }

        private List<Line> lines(List<LineAlignment.Hunk> ourHunks, List<LineAlignment.Hunk> theirHunks) {
            var lines = new ArrayList<Line>();
            var covered = new boolean[base.lines()];
            for (var i = 0; i <= base.lines(); i++) {
                addHunksAt(i, ourHunks, Side.OUR, lines, covered);
                addHunksAt(i, theirHunks, Side.THEIR, lines, covered);
                if (i < base.lines() && !covered[i]) {
                    lines.add(new Line(i, alignments.our().sideOf()[i], alignments.their().sideOf()[i]));
                }
            }
            return lines;
        }

        /**
         * Adds the lines of a revision's structural changes made before a line of BASE revision, and marks the lines of
         * BASE revision they replace.
         */
        private static void addHunksAt(int baseLine,
                                       List<LineAlignment.Hunk> hunks,
                                       Side side,
                                       List<Line> lines,
                                       boolean[] covered) {
            for (LineAlignment.Hunk hunk : hunks) {
                if (hunk.baseFrom() == baseLine) {
                    for (var line = hunk.sideFrom(); line < hunk.sideTo(); line++) {
                        lines.add(Line.of(side, line));
                    }
                    Arrays.fill(covered, hunk.baseFrom(), hunk.baseTo(), true);
                }
            }
        }

        /**
         * Tells whether two structural changes touch the same lines of BASE revision, or one of them adds lines next to
         * lines the other one adds or deletes.
         */
        private static boolean overlap(LineAlignment.Hunk a, LineAlignment.Hunk b) {
            if (a.baseFrom() < b.baseTo() && b.baseFrom() < a.baseTo()) {
                return true;
            }
            // lines added next to lines the other one adds or deletes cannot be ordered
            var adds = a.sideFrom() < a.sideTo() || b.sideFrom() < b.sideTo();
            return adds && a.baseFrom() <= b.baseTo() && b.baseFrom() <= a.baseTo();
        }

        /**
         * Tells whether both revisions made the same structural change: the same lines of BASE revision replaced by
         * the same lines.
         */
        private boolean sameHunk(LineAlignment.Hunk ourHunk, LineAlignment.Hunk theirHunk) {
            if (ourHunk.baseFrom() != theirHunk.baseFrom() || ourHunk.baseTo() != theirHunk.baseTo()
                    || ourHunk.sideTo() - ourHunk.sideFrom() != theirHunk.sideTo() - theirHunk.sideFrom()) {
                return false;
            }
            for (var i = 0; i < ourHunk.sideTo() - ourHunk.sideFrom(); i++) {
                if (!our.equalLine(ourHunk.sideFrom() + i, their, theirHunk.sideFrom() + i)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Tells whether the other revision changed a line that a structural change of one revision deletes or
         * replaces.
         */
        private boolean changedUnder(List<LineAlignment.Hunk> hunks, View other, LineAlignment otherAlignment) {
            for (LineAlignment.Hunk hunk : hunks) {
                for (var line = hunk.baseFrom(); line < hunk.baseTo(); line++) {
                    var otherLine = otherAlignment.sideOf()[line];
                    if (otherLine >= 0 && !base.equalLine(line, other, otherLine)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * The cells and the merged cells of a merged table at its place in the merged sheet.
     */
    private final class TableOutput {

        private final Views views;
        private final Lines lines;
        private final CellRangeAddress target;
        private final boolean transposed;
        private final Map<CellAddress, CellSource> cells = new HashMap<>();
        private final List<CellRangeAddress> regions = new ArrayList<>();
        private final Map<Side, int[]> lineOf = new EnumMap<>(Side.class);

        private TableOutput(Views views, Lines lines, CellRangeAddress target) {
            this.views = views;
            this.lines = lines;
            this.target = target;
            this.transposed = views.any().transposed();
        }

        /**
         * @return {@code false} if the cells of a line conflict, or a change crosses merged cells
         */
        boolean build() {
            for (Side side : REVISIONS) {
                var view = views.of(side);
                if (view != null) {
                    var lineOfSide = new int[view.lines()];
                    Arrays.fill(lineOfSide, -1);
                    lineOf.put(side, lineOfSide);
                }
            }
            for (var i = 0; i < lines.lines().size(); i++) {
                var line = lines.lines().get(i);
                for (Side side : REVISIONS) {
                    if (line.of(side) >= 0) {
                        lineOf.get(side)[line.of(side)] = i;
                    }
                }
                if (!buildLine(i, line)) {
                    return false;
                }
            }
            return buildRegions();
        }

        private boolean buildLine(int index, Line line) {
            var ourView = views.our();
            var theirView = views.their();
            var positions = views.any().positions();
            for (var position = 0; position < positions; position++) {
                CellSource source;
                if (line.base() >= 0) {
                    var parts = mergeCell(line, position);
                    if (parts == null) {
                        return false;
                    }
                    source = new CellSource(ourView.address(line.our(), position),
                            parts.isEmpty() ? null : theirView.address(line.their(), position),
                            parts);
                } else if (line.our() >= 0) {
                    source = new CellSource(ourView.address(line.our(), position), null, Set.of());
                } else {
                    source = new CellSource(null, theirView.address(line.their(), position), ALL_PARTS);
                }
                cells.put(address(index, position), source);
            }
            return true;
        }

        private @Nullable Set<CellPart> mergeCell(Line line, int position) {
            var baseView = views.base();
            var ourView = views.our();
            var theirView = views.their();
            baseView.cursor().cell = baseView.cell(line.base(), position);
            ourView.cursor().cell = ourView.cell(line.our(), position);
            theirView.cursor().cell = theirView.cell(line.their(), position);
            var parts = XlsSheetMerger.theirParts(baseView.cursor(), ourView.cursor(), theirView.cursor());
            baseView.cursor().cell = ourView.cursor().cell = theirView.cursor().cell = null;
            return parts;
        }

        private CellAddress address(int line, int position) {
            return XlsTableMerger.address(target, transposed, line, position);
        }

        /**
         * Places the merged cells of the revisions in the merged table.
         *
         * <p>The merged cells over lines of BASE revision are merged as a whole, the way a part of a cell is: they are
         * taken from the revision that changed them, by the lines of that revision. Merged cells a revision stretched
         * over lines it inserted, or shrank by lines it deleted, are changed in that revision. Merged cells over lines
         * a revision inserted only come with those lines.
         *
         * @return {@code false} if both revisions changed the merged cells differently, merged cells do not stay over
         * lines next to each other, or they cover a value another revision wrote
         */
        private boolean buildRegions() {
            var keys = new EnumMap<Side, Set<RegionKey>>(Side.class);
            var spans = new EnumMap<Side, List<Span>>(Side.class);
            for (Side side : Side.values()) {
                var view = views.of(side);
                if (view != null && !sortRegions(side, view, keys, spans)) {
                    return false;
                }
            }
            if (views.base() == null) {
                return true;
            }
            var choice = XlsSheetMerger.choose(keys.get(Side.BASE), keys.get(Side.OUR), keys.get(Side.THEIR),
                    Object::equals);
            if (choice == Choice.CONFLICT) {
                return false;
            }
            var owner = choice == Choice.THEIR ? Side.THEIR : Side.OUR;
            for (Span span : spans.get(owner)) {
                if (!place(span, owner)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Sorts the merged cells of a revision. Those over lines of BASE revision are kept with their keys, to be
         * merged as a whole. The others are placed at once.
         *
         * @return {@code false} if merged cells cannot be placed
         */
        private boolean sortRegions(Side side,
                                    View view,
                                    Map<Side, Set<RegionKey>> keys,
                                    Map<Side, List<Span>> spans) {
            var sideKeys = new HashSet<RegionKey>();
            var sideSpans = new ArrayList<Span>();
            keys.put(side, sideKeys);
            spans.put(side, sideSpans);
            for (Span span : view.regions()) {
                var key = key(side, span);
                if (key != null) {
                    sideKeys.add(key);
                    sideSpans.add(span);
                } else if (!place(span, side)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * The lines of BASE revision merged cells of a revision cover, and the number of lines they cover in that
         * revision.
         *
         * @return the key, or {@code null} for merged cells over lines the revision inserted only, or of a table
         * taken whole from one revision
         */
        private @Nullable RegionKey key(Side side, Span span) {
            var length = span.lastLine() - span.firstLine() + 1;
            if (side == Side.BASE) {
                return new RegionKey(span.firstLine(), span.lastLine(), span.firstPosition(), span.lastPosition(),
                        length);
            } else if (lines.alignments() == null) {
                return null;
            }
            var baseOf = lines.alignments().of(side).baseOf();
            var first = -1;
            var last = -1;
            for (var line = span.firstLine(); line <= span.lastLine(); line++) {
                if (baseOf[line] >= 0) {
                    first = first < 0 ? baseOf[line] : first;
                    last = baseOf[line];
                }
            }
            return first < 0
                    ? null
                    : new RegionKey(first, last, span.firstPosition(), span.lastPosition(), length);
        }

        /**
         * Places merged cells of a revision in the merged table by the lines of that revision.
         *
         * @return {@code false} if those lines are not next to each other in the merged table, or the merged cells
         * cover a value another revision wrote
         */
        private boolean place(Span span, Side owner) {
            var lineOfSide = lineOf.get(owner);
            var first = lineOfSide[span.firstLine()];
            for (var line = span.firstLine(); line <= span.lastLine(); line++) {
                if (first < 0 || lineOfSide[line] != first + line - span.firstLine()) {
                    return false;
                }
            }
            var last = first + span.lastLine() - span.firstLine();
            for (var line = first; line <= last; line++) {
                for (var position = span.firstPosition(); position <= span.lastPosition(); position++) {
                    var covered = line != first || position != span.firstPosition();
                    if (covered && hidesValue(line, position, owner)) {
                        return false;
                    }
                }
            }
            regions.add(area(address(first, span.firstPosition()), address(last, span.lastPosition())));
            return true;
        }

        /**
         * Tells whether a cell of the merged table under merged cells of one revision takes a value the other revision
         * shows. A value the other revision keeps under merged cells of its own is hidden there as well.
         */
        private boolean hidesValue(int line, int position, Side owner) {
            var source = cells.get(address(line, position));
            var fromTheir = source.our() == null || source.parts().contains(CellPart.VALUE);
            var valueOwner = fromTheir ? Side.THEIR : Side.OUR;
            var view = views.of(valueOwner);
            var address = fromTheir ? source.their() : source.our();
            if (valueOwner == owner || view == null || address == null) {
                return false;
            }
            var cursor = view.cursor();
            return XlsSheetsMatcher.hasValue(SheetUtil.getCell(cursor.sheet, address.getRow(), address.getColumn()))
                    && !isCovered(cursor, address);
        }

        /**
         * Tells whether a cell lies under merged cells of its sheet, other than their first cell.
         */
        private static boolean isCovered(Cursor cursor, CellAddress address) {
            var region = cursor.getMergedRegion(address.getRow(), address.getColumn());
            return region != null
                    && (region.getFirstRow() != address.getRow() || region.getFirstColumn() != address.getColumn());
        }

        /**
         * The areas whose content moves: the runs of lines of a revision that land at other addresses than they have
         * in that revision, and the addresses they land at.
         */
        List<CellRangeAddress> moved(Side side) {
            var view = views.of(side);
            if (view == null) {
                return List.of();
            }
            var shifted = view.table().getFirstRow() != target.getFirstRow()
                    || view.table().getFirstColumn() != target.getFirstColumn();
            var areas = new ArrayList<CellRangeAddress>();
            var runStart = -1;
            for (var i = 0; i <= lines.lines().size(); i++) {
                var line = i < lines.lines().size() ? lines.lines().get(i).of(side) : -1;
                var moves = line >= 0 && (shifted || line != i);
                var continues = moves && runStart >= 0 && line == lines.lines().get(i - 1).of(side) + 1;
                if (runStart >= 0 && !continues) {
                    addRun(view, runStart, i - 1, side, areas);
                    runStart = -1;
                }
                if (moves && runStart < 0) {
                    runStart = i;
                }
            }
            return areas;
        }

        /**
         * Adds the lines of a revision that land at the lines {@code from..to} of the merged table, at both places.
         */
        private void addRun(View view, int from, int to, Side side, List<CellRangeAddress> areas) {
            var last = view.positions() - 1;
            areas.add(area(view.address(lines.lines().get(from).of(side), 0),
                    view.address(lines.lines().get(to).of(side), last)));
            areas.add(area(address(from, 0), address(to, last)));
        }
    }
}
