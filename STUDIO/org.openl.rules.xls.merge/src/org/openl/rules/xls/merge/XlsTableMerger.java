package org.openl.rules.xls.merge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.poi.hssf.usermodel.HSSFComment;
import org.apache.poi.hssf.usermodel.HSSFPatriarch;
import org.apache.poi.hssf.usermodel.HSSFShape;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeUtil;
import org.apache.poi.ss.util.SheetUtil;
import org.apache.poi.xssf.usermodel.XSSFShape;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import org.openl.rules.xls.merge.XlsSheetMerger.Choice;
import org.openl.rules.xls.merge.diff.CellConflict;
import org.openl.rules.xls.merge.diff.CellPart;
import org.openl.rules.xls.merge.diff.CellResolution;
import org.openl.rules.xls.merge.diff.CellResolutionException;
import org.openl.rules.xls.merge.diff.CellSource;
import org.openl.rules.xls.merge.diff.Resolution;
import org.openl.rules.xls.merge.diff.SheetConflictReason;
import org.openl.rules.xls.merge.diff.TableCell;
import org.openl.rules.xls.merge.diff.TableCells;
import org.openl.rules.xls.merge.diff.TableConflict;
import org.openl.rules.xls.merge.diff.TableConflictReason;

/**
 * Merges a sheet changed in both revisions table by table against the base revision.
 *
 * <p>Each revision of the sheet is split into tables, and the tables of BASE revision are found in the other
 * revisions. A table added, removed, changed or moved in one revision only takes the change of that revision, and a
 * table both revisions added holding the same is kept once, where OUR revision has it. A table changed in both
 * revisions is merged line by line, the way lines of text are merged: its rows, or its columns, are aligned with BASE
 * revision, and the lines inserted, deleted or changed in one revision only are taken from it. The cells of a line
 * changed in both revisions are merged part by part.
 *
 * <p>Lines both revisions inserted at the same place are all kept: those of OUR revision first, then those of THEIR
 * revision. A line both revisions inserted there holding the same values and comments is kept once, as OUR revision
 * has it.
 *
 * <p>A revision that only pushes its tables up or down, by rows it inserts into tables or between them, does not move
 * them on purpose: the merged sheet follows the places the other revision gives the tables, pushed by those rows. The
 * rows a revision inserted into a table push the tables under it down as well when the table, grown by the rows of
 * both revisions, would touch them otherwise.
 *
 * <p>The sheet stays a conflict when both revisions change the same lines differently, both move a table on purpose
 * to different places, or delete a table the other revision changes. It stays a conflict as well when one revision
 * changes the rows and the other the columns of one table, when a change crosses merged cells, when merged tables
 * would touch each other, and when moved content is referred to by a formula, a defined name, a data validation, a
 * conditional formatting or a hyperlink.
 *
 * <p>A table whose lines merge while some of its cells conflict can be resolved cell by cell: a survey lists the
 * merged table with its conflicting cells, and a resolution takes each conflicting cell from the revision chosen for
 * it.
 */
@NullMarked
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
final class XlsTableMerger {

    private static final Set<CellPart> ALL_PARTS = Set.copyOf(EnumSet.allOf(CellPart.class));
    private static final CellSource CLEARED = new CellSource(null, null, Set.of());
    /**
     * The first cell of the rows of a table that hold its properties, right under its header.
     */
    private static final String PROPERTIES = "properties";
    /**
     * The name of a column of a decision table: its kind, such as {@code C} for a condition or {@code RET} for the
     * return value, and its number.
     */
    private static final Pattern COLUMN_NAME = Pattern.compile("([A-Za-z]+)(\\d+)");

    private final Cursor base;
    private final Cursor our;
    private final Cursor their;
    /**
     * Tells whether THEIR revision changed or created a sheet, by its name.
     */
    private final Predicate<String> changedInTheirs;
    /**
     * Resolves a table both revisions changed in ways the merge cannot combine, when anything resolves it.
     */
    private final Conflicts conflicts;
    /**
     * The tables the three revisions split into.
     */
    private final Tables split;
    /**
     * The rows one revision pushed its tables by, carried over onto the places of the other revision, or
     * {@code null} when the places are not combined.
     */
    private @Nullable Pushes pushes;

    /**
     * Tables of OUR revision whose cells are cleared before the merged tables are written.
     */
    private final List<CellRangeAddress> clearedTables = new ArrayList<>();
    /**
     * Merged tables written into OUR revision.
     */
    private final List<TableOutput> outputs = new ArrayList<>();
    /**
     * The rows of the tables both revisions kept, aligned with BASE revision, by the table of BASE revision.
     */
    private final Map<CellRangeAddress, Optional<Alignments>> rowAlignments = new HashMap<>();
    /**
     * The mergers of the rows of the tables both revisions kept, by the table of BASE revision.
     */
    private final Map<CellRangeAddress, Optional<LineMerger>> rowMergers = new HashMap<>();
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
     * Areas of BASE revision whose content lands at other addresses: the tables a revision moved, resized or removed,
     * and the content taken from BASE revision that the merge writes elsewhere.
     */
    private final List<CellRangeAddress> baseMoved = new ArrayList<>();
    /**
     * The tables of the three revisions: a cell outside all of them holds no value in any revision.
     */
    private final List<CellRangeAddress> tables = new ArrayList<>();
    /**
     * The conflicting tables a survey merged cell by cell, by the table of BASE revision.
     */
    private final Map<CellRangeAddress, TableCells> mergedByCells = new HashMap<>();

    /**
     * The cells and the merged cells of a sheet merged table by table.
     *
     * @param cells         cells of OUR revision that change, with the content they take
     * @param mergedRegions all the merged cells of the merged sheet, or {@code null} to keep those of OUR revision
     * @param theirMoved    areas of THEIR revision whose content lands at other addresses, and the addresses it lands
     *                      at
     */
    record Content(Map<CellAddress, CellSource> cells,
                   @Nullable List<CellRangeAddress> mergedRegions,
                   List<CellRangeAddress> theirMoved) {
    }

    /**
     * The tables the three revisions of a sheet split into, and the tables of OUR and THEIR revisions found for the
     * tables of BASE revision.
     *
     * @param ourPairs   the table of OUR revision by the table of BASE revision it is
     * @param theirPairs the table of THEIR revision by the table of BASE revision it is
     */
    private record Tables(List<CellRangeAddress> base,
                          List<CellRangeAddress> our,
                          List<CellRangeAddress> their,
                          Map<CellRangeAddress, CellRangeAddress> ourPairs,
                          Map<CellRangeAddress, CellRangeAddress> theirPairs) {

        static Tables of(Cursor base, Cursor our, Cursor their) {
            var baseTables = XlsSheetTables.split(base.sheet);
            var ourTables = XlsSheetTables.split(our.sheet);
            var theirTables = XlsSheetTables.split(their.sheet);
            return new Tables(baseTables,
                    ourTables,
                    theirTables,
                    XlsSheetTables.match(baseTables, base.sheet, ourTables, our.sheet),
                    XlsSheetTables.match(baseTables, base.sheet, theirTables, their.sheet));
        }
    }

    /**
     * Plans the table by table merge of a sheet changed in both revisions.
     *
     * <p>A table both revisions changed in ways that cannot be combined is taken whole from the revision the conflicts
     * resolve it with. When nothing resolves it, the sheet stays a conflict.
     *
     * <p>A table both revisions grew, each into the empty rows under it, may come to touch the table under it once it
     * holds the rows of both. The rows one revision inserted then push the tables under it down, as they do when both
     * revisions moved a table, unless a revision moved a table on purpose, whose move the pushed places would lose.
     *
     * @param changedInTheirs tells whether THEIR revision changed or created a sheet, by its name
     * @param conflicts       resolves the tables that conflict, and learns why a sheet stays a conflict
     * @return what changes in OUR revision of the sheet, or nothing if the changes conflict
     */
    static Optional<Content> plan(Cursor base,
                                  Cursor our,
                                  Cursor their,
                                  Predicate<String> changedInTheirs,
                                  Conflicts conflicts) {
        var mark = conflicts.mark();
        var merger = new XlsTableMerger(base, our, their, changedInTheirs, conflicts, Tables.of(base, our, their));
        if (merger.merge(Placing.MOVED_APART)) {
            return Optional.of(merger.content());
        } else if (conflicts.refusal() == null) {
            return Optional.empty();
        } else if (merger.pushes != null) {
            // The combined places do not fit together: the tables both revisions moved are conflicts of their own then.
            return merger.replan(mark, Placing.APART);
        } else if (conflicts.refusal() != SheetConflictReason.TOUCHING_TABLES || !merger.pushedOnly()) {
            return Optional.empty();
        }
        var pushed = merger.replan(mark, Placing.PUSHED);
        if (pushed.isEmpty()) {
            // The sheet stays the conflict it was found to be.
            merger.replan(mark, Placing.MOVED_APART);
        }
        return pushed;
    }

    /**
     * Tells whether the rows one revision inserted may push down the tables the other revision has under them: one
     * revision only pushes its tables, and neither revision moved a table on purpose.
     */
    private boolean pushedOnly() {
        var baseTables = split.base();
        var ourPairs = split.ourPairs();
        var theirPairs = split.theirPairs();
        SharedRows shared = (table, line) -> sharedRows(table, line, ourPairs, theirPairs);
        return Pushes.movedOnPurpose(baseTables, ourPairs, shared).isEmpty()
                && Pushes.movedOnPurpose(baseTables, theirPairs, shared).isEmpty()
                && Pushes.of(baseTables, ourPairs, theirPairs, shared) != null;
    }

    /**
     * Plans the merge of the sheet anew, with the places combined as given, once what this merge noted is forgotten.
     */
    private Optional<Content> replan(Conflicts.Mark mark, Placing placing) {
        conflicts.rewind(mark);
        var merger = new XlsTableMerger(base, our, their, changedInTheirs, conflicts, split);
        return merger.merge(placing) ? Optional.of(merger.content()) : Optional.empty();
    }

    /**
     * When the merged sheet combines the places both revisions gave the tables, one revision only pushing its tables
     * by the rows it inserted or deleted.
     */
    private enum Placing {
        /**
         * Never: a table takes the place a revision moved it to.
         */
        APART,
        /**
         * When both revisions moved a table, each to another place.
         */
        MOVED_APART,
        /**
         * Always, so that the rows a revision inserted push down the tables the other revision has under them.
         */
        PUSHED
    }

    private boolean merge(Placing placing) {
        var baseTables = split.base();
        tables.addAll(baseTables);
        tables.addAll(split.our());
        tables.addAll(split.their());
        var ourPairs = split.ourPairs();
        var theirPairs = split.theirPairs();
        var combines = switch (placing) {
            case APART -> false;
            case MOVED_APART -> movedApart(baseTables, ourPairs, theirPairs);
            case PUSHED -> true;
        };
        if (combines) {
            pushes = Pushes.of(baseTables,
                    ourPairs,
                    theirPairs,
                    (table, line) -> sharedRows(table, line, ourPairs, theirPairs));
        }
        return mergeBaseTables() && mergeCreatedTables() && fitTogether();
    }

    /**
     * Merges each table of BASE revision with what the revisions made of it.
     *
     * @return whether every table merged, or the conflicts resolved it
     */
    private boolean mergeBaseTables() {
        var ourPairs = split.ourPairs();
        var theirPairs = split.theirPairs();
        for (CellRangeAddress baseTable : split.base()) {
            var ourTable = ourPairs.get(baseTable);
            var theirTable = theirPairs.get(baseTable);
            if (!baseTable.equals(ourTable) || !baseTable.equals(theirTable)) {
                baseMoved.add(baseTable);
            }
            var reason = mergeTable(baseTable, ourTable, theirTable);
            if (reason != null && !resolve(new TableConflict(idOf(baseTable),
                    ourTable,
                    baseTable,
                    theirTable,
                    reason,
                    mergedByCells.get(baseTable)))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Merges the tables the revisions created. A table both revisions created holding the same is kept once, where OUR
     * revision has it.
     *
     * @return whether every created table merged, or the conflicts resolved it
     */
    private boolean mergeCreatedTables() {
        var ourCreated = created(split.our(), split.ourPairs());
        var theirCreated = created(split.their(), split.theirPairs());
        var createdAlike = leaveOutCreatedAlike(our, their, ourCreated, theirCreated);
        for (List<CellRangeAddress> group : createdGroups(theirCreated, ourCreated)) {
            if (!mergeCreated(group, ourCreated)) {
                return false;
            }
        }
        for (CellRangeAddress table : ourCreated) {
            if (takeOurs(table, createdAlike.get(table), start(createdPlace(Side.OUR, table))) != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tells whether the merged tables fit together: no two of them touch, and nothing refers to content that moves.
     * Otherwise the conflicts learn why the sheet stays a conflict.
     */
    private boolean fitTogether() {
        if (!isSeparated()) {
            conflicts.refuse(SheetConflictReason.TOUCHING_TABLES);
            return false;
        } else if (refersToMoved() || baseRefersToMoved()) {
            conflicts.refuse(SheetConflictReason.MOVED_CONTENT);
            return false;
        }
        return true;
    }

    /**
     * Tells whether both revisions moved a table of BASE revision, each to another place.
     */
    private static boolean movedApart(List<CellRangeAddress> baseTables,
                                      Map<CellRangeAddress, CellRangeAddress> ourPairs,
                                      Map<CellRangeAddress, CellRangeAddress> theirPairs) {
        return baseTables.stream().anyMatch(table -> {
            var ourTable = ourPairs.get(table);
            var theirTable = theirPairs.get(table);
            return ourTable != null && theirTable != null && chosenStart(table, ourTable, theirTable) == null;
        });
    }

    /**
     * The rows both revisions inserted alike into a table of BASE revision above one of its rows, less the rows both
     * deleted alike there.
     *
     * @param line the row of the table in BASE revision, counted from its first row
     */
    private int sharedRows(CellRangeAddress baseTable,
                           int line,
                           Map<CellRangeAddress, CellRangeAddress> ourPairs,
                           Map<CellRangeAddress, CellRangeAddress> theirPairs) {
        return rowMergers
                .computeIfAbsent(baseTable, table -> Optional.ofNullable(rowMerger(table, ourPairs, theirPairs)))
                .map(merger -> merger.sharedLines(line))
                .orElse(0);
    }

    /**
     * Merges the rows of a table both revisions kept.
     *
     * @return the merger, or {@code null} when a revision removed the table or too many rows changed to align them
     */
    private @Nullable LineMerger rowMerger(CellRangeAddress baseTable,
                                           Map<CellRangeAddress, CellRangeAddress> ourPairs,
                                           Map<CellRangeAddress, CellRangeAddress> theirPairs) {
        var ourTable = ourPairs.get(baseTable);
        var theirTable = theirPairs.get(baseTable);
        if (ourTable == null || theirTable == null) {
            return null;
        }
        var baseView = view(base, baseTable);
        var ourView = view(our, ourTable);
        var theirView = view(their, theirTable);
        var alignments = rowAlignments(baseTable, new Views(baseView, ourView, theirView));
        return alignments == null ? null : new LineMerger(baseView, ourView, theirView, alignments);
    }

    /**
     * Tells whether both revisions created a table holding the same at different places. Such a sheet is merged table
     * by table, which keeps the table once.
     */
    static boolean createdAlikeElsewhere(Cursor base, Cursor our, Cursor their) {
        var baseTables = XlsSheetTables.split(base.sheet);
        var theirCreated = created(baseTables, base, their);
        return !theirCreated.isEmpty()
                && !leaveOutCreatedAlike(our, their, created(baseTables, base, our), theirCreated).isEmpty();
    }

    /**
     * The tables a revision created, which BASE revision has not got.
     */
    private static List<CellRangeAddress> created(List<CellRangeAddress> baseTables, Cursor base, Cursor side) {
        var tables = XlsSheetTables.split(side.sheet);
        return created(tables, XlsSheetTables.match(baseTables, base.sheet, tables, side.sheet));
    }

    /**
     * The tables of a revision that BASE revision has not got.
     *
     * @param pairs the table of the revision by the table of BASE revision
     */
    private static List<CellRangeAddress> created(List<CellRangeAddress> tables,
                                                  Map<CellRangeAddress, CellRangeAddress> pairs) {
        var created = new ArrayList<>(tables);
        created.removeAll(new HashSet<>(pairs.values()));
        return created;
    }

    /**
     * Leaves out the tables THEIR revision created that OUR revision created too, holding the same: OUR revision has
     * them already. A table both revisions created at one place is merged already.
     *
     * <p>A table OUR revision created at another place stands for one table of THEIR revision. A table that touches a
     * table THEIR revision created stands for none: the tables that touch are resolved together.
     *
     * @return the table of THEIR revision left out by the table of OUR revision at another place that stands for it
     */
    private static Map<CellRangeAddress, CellRangeAddress> leaveOutCreatedAlike(Cursor our,
                                                                                Cursor their,
                                                                                List<CellRangeAddress> ourCreated,
                                                                                List<CellRangeAddress> theirCreated) {
        theirCreated.removeIf(table -> ourCreated.contains(table) && holdAlike(view(our, table), view(their, table)));
        var pairs = new HashMap<CellRangeAddress, CellRangeAddress>();
        var paired = new HashSet<CellRangeAddress>();
        for (CellRangeAddress ourTable : ourCreated) {
            if (theirCreated.stream().noneMatch(table -> XlsSheetTables.touch(ourTable, table))) {
                theirCreated.stream()
                        .filter(table -> !paired.contains(table) && holdAlike(view(our, ourTable), view(their, table)))
                        .findFirst()
                        .ifPresent(table -> {
                            paired.add(table);
                            pairs.put(ourTable, table);
                        });
            }
        }
        theirCreated.removeAll(paired);
        return pairs;
    }

    /**
     * Where the merge puts a table only one revision created: at its place, which the rows the other revision pushed
     * its tables by move when the merged sheet follows the places of this revision.
     */
    private CellRangeAddress createdPlace(Side side, CellRangeAddress table) {
        return pushes != null && pushes.layout() == side ? pushes.pushed(table) : table;
    }

    /**
     * Groups the tables THEIR revision created that touch one table OUR revision created, so that each group is
     * resolved as one conflict. A table that touches no table of OUR revision is a group of its own.
     */
    private static List<List<CellRangeAddress>> createdGroups(List<CellRangeAddress> theirCreated,
                                                              List<CellRangeAddress> ourCreated) {
        var groups = new ArrayList<List<CellRangeAddress>>();
        for (CellRangeAddress table : theirCreated) {
            var joined = groups.stream().filter(group -> touchOneTable(group, table, ourCreated)).toList();
            var group = new ArrayList<CellRangeAddress>();
            joined.forEach(group::addAll);
            group.add(table);
            groups.removeIf(joined::contains);
            groups.add(group);
        }
        return groups;
    }

    /**
     * Tells whether a table THEIR revision created touches a table OUR revision created that a group of tables
     * touches too.
     */
    private static boolean touchOneTable(List<CellRangeAddress> group,
                                         CellRangeAddress table,
                                         List<CellRangeAddress> ourCreated) {
        return ourCreated.stream()
                .anyMatch(ourTable -> XlsSheetTables.touch(ourTable, table)
                        && group.stream().anyMatch(other -> XlsSheetTables.touch(ourTable, other)));
    }

    /**
     * Merges tables THEIR revision created: they join the merged sheet. The tables OUR revision created over them or
     * next to them conflict with them, and the conflict is resolved for all of them at once.
     *
     * @param tables     a group of tables THEIR revision created, from top to bottom
     * @param ourCreated the tables OUR revision created; tables taken from THEIR revision in place of some of them
     *                   leave them out
     * @return {@code false} if the sheet stays a conflict
     */
    private boolean mergeCreated(List<CellRangeAddress> tables, List<CellRangeAddress> ourCreated) {
        var touched = ourCreated.stream()
                .filter(ourTable -> tables.stream().anyMatch(table -> XlsSheetTables.touch(ourTable, table)))
                .toList();
        if (touched.isEmpty()) {
            return writeCreated(tables);
        }
        var resolution = conflicts.resolve(new TableConflict(addedIdOf(tables.getFirst()),
                enclosing(touched),
                null,
                enclosing(tables),
                TableConflictReason.ADDED_IN_BOTH));
        if (resolution.isEmpty()) {
            return false;
        } else if (resolution.get() == Resolution.OURS) {
            return true;
        }
        ourCreated.removeAll(touched);
        clearedTables.addAll(touched);
        return resolution.get() == Resolution.BASE || writeCreated(tables);
    }

    /**
     * Writes tables THEIR revision created, each at its place.
     *
     * @return {@code false} if the sheet stays a conflict
     */
    private boolean writeCreated(List<CellRangeAddress> tables) {
        return tables.stream()
                .allMatch(table -> writeWhole(null,
                        Side.THEIR,
                        view(their, table),
                        null,
                        createdPlace(Side.THEIR, table)) == null);
    }

    private static CellRangeAddress enclosing(List<CellRangeAddress> tables) {
        return tables.stream().reduce(CellRangeUtil::createEnclosingCellRange).orElseThrow();
    }

    /**
     * The identifier of the conflict of a table: the sheet and the place of the table in BASE revision. Tables of one
     * revision never overlap, and a sheet name never holds a slash.
     */
    private String idOf(CellRangeAddress table) {
        return base.sheet.getSheetName() + '/' + table.formatAsString();
    }

    /**
     * The identifier of the conflict of tables both revisions added: the sheet, {@code added}, and the place of the
     * first table THEIR revision added there. It differs from the identifier of any table of BASE revision, which may
     * have stood at the same place.
     */
    private String addedIdOf(CellRangeAddress table) {
        return base.sheet.getSheetName() + "/added/" + table.formatAsString();
    }

    /**
     * Resolves a table both revisions changed in ways that cannot be combined: the table is taken whole from the
     * revision the conflicts resolve it with.
     *
     * <p>The table lands where the merge puts it: at the place one revision moved it to, as a table only one revision
     * changed does. When both revisions moved it, or one removed it, the version taken keeps its own place.
     *
     * <p>A table taken whole is always written: its lines and its merged cells all come from the one revision, so
     * none of them conflicts. Only a conflict nothing resolves leaves the sheet a conflict.
     *
     * @return {@code false} if the sheet stays a conflict
     */
    private boolean resolve(TableConflict conflict) {
        var resolution = conflicts.resolve(conflict);
        if (resolution.isEmpty()) {
            return false;
        }
        var ourTable = conflict.ours();
        var theirTable = conflict.theirs();
        var start = mergedStart(conflict);
        return switch (resolution.get()) {
            case OURS -> ourTable == null || takeOurs(ourTable, theirTable, start) == null;
            case THEIRS -> takeTheirs(ourTable, theirTable, start);
            case BASE -> takeBase(ourTable, Objects.requireNonNull(conflict.base()), theirTable, start);
        };
    }

    /**
     * Where the merge puts a table both revisions kept: at the place a revision moved it to, or where both moved it
     * alike.
     *
     * @return the top left cell of the table, or {@code null} when both revisions moved the table on purpose to
     * different places or one of them removed it
     */
    private @Nullable CellAddress mergedStart(TableConflict conflict) {
        var baseTable = conflict.base();
        var ourTable = conflict.ours();
        var theirTable = conflict.theirs();
        if (baseTable == null || ourTable == null || theirTable == null) {
            return null;
        }
        return mergedStart(baseTable, ourTable, theirTable);
    }

    /**
     * Where the merge puts a table both revisions kept: at the place a revision moved it to, where both moved it
     * alike, or where the rows one revision pushed its tables by take the place the other revision gives it.
     *
     * @return the top left cell of the table, or {@code null} when both revisions moved it to places that cannot be
     * combined
     */
    private @Nullable CellAddress mergedStart(CellRangeAddress baseTable,
                                              CellRangeAddress ourTable,
                                              CellRangeAddress theirTable) {
        if (pushes != null && !pushes.movedByBoth().contains(baseTable)) {
            return pushes.start(pushes.layout() == Side.THEIR ? theirTable : ourTable);
        }
        return chosenStart(baseTable, ourTable, theirTable);
    }

    /**
     * Where a table both revisions kept stands once the place one revision moved it to is taken.
     *
     * @return the top left cell of the table, or {@code null} when both revisions moved it to different places
     */
    private static @Nullable CellAddress chosenStart(CellRangeAddress baseTable,
                                                     CellRangeAddress ourTable,
                                                     CellRangeAddress theirTable) {
        return switch (XlsSheetMerger.choose(start(baseTable), start(ourTable), start(theirTable), Object::equals)) {
            case OUR -> start(ourTable);
            case THEIR -> start(theirTable);
            case CONFLICT -> null;
        };
    }

    /**
     * Takes a table whole from OUR revision, where the merge puts it.
     *
     * @param start where the merge puts the table, or {@code null} to keep it at its place
     * @return why the table cannot be written, or {@code null} when it is
     */
    private @Nullable TableConflictReason takeOurs(CellRangeAddress ourTable,
                                                   @Nullable CellRangeAddress theirTable,
                                                   @Nullable CellAddress start) {
        if (start == null || start.equals(start(ourTable))) {
            layout.add(ourTable);
            otherMoved(Side.OUR, theirTable, ourTable);
            return null;
        }
        return writeWhole(ourTable, Side.OUR, view(our, ourTable), theirTable, place(start, ourTable));
    }

    /**
     * Takes a table whole from THEIR revision, where the merge puts it, or leaves it out when THEIR revision removed
     * it.
     *
     * @param start where the merge puts the table, or {@code null} to put it where THEIR revision has it
     * @return {@code false} if the sheet stays a conflict
     */
    private boolean takeTheirs(@Nullable CellRangeAddress ourTable,
                               @Nullable CellRangeAddress theirTable,
                               @Nullable CellAddress start) {
        if (theirTable != null) {
            var target = start == null ? theirTable : place(start, theirTable);
            return writeWhole(ourTable, Side.THEIR, view(their, theirTable), ourTable, target) == null;
        }
        if (ourTable != null) {
            clearedTables.add(ourTable);
        }
        return true;
    }

    /**
     * Takes a table whole from BASE revision, where the merge puts it, discarding the changes of both revisions.
     *
     * @param start where the merge puts the table, or {@code null} to put it where BASE revision has it
     * @return {@code false} if the sheet stays a conflict
     */
    private boolean takeBase(@Nullable CellRangeAddress ourTable,
                             CellRangeAddress baseTable,
                             @Nullable CellRangeAddress theirTable,
                             @Nullable CellAddress start) {
        var target = start == null ? baseTable : place(start, baseTable);
        if (writeWhole(ourTable, Side.BASE, view(base, baseTable), ourTable, target) != null) {
            return false;
        }
        if (target.equals(baseTable)) {
            // The content of BASE revision is back at its own addresses.
            baseMoved.remove(baseTable);
        }
        otherMoved(Side.OUR, theirTable, target);
        return true;
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
                if (address.equals(source.our()) && source.parts().isEmpty() && source.typed() == null) {
                    // the cell keeps its place and its content
                    cells.remove(address);
                } else {
                    cells.put(address, source);
                }
            });
        }
        looseCells(cells);
        if (clearedTables.isEmpty() && outputs.isEmpty()) {
            return new Content(cells, null, theirMoved);
        }
        var regions = our.getMergedRegions()
                .stream()
                .filter(region -> clearedTables.stream().noneMatch(table -> table.intersects(region)))
                .collect(Collectors.toCollection(ArrayList::new));
        outputs.forEach(output -> regions.addAll(output.regions));
        return new Content(cells, regions, theirMoved);
    }

    /**
     * Merges the cells outside every table, which hold no value in any revision, cell by cell: the style or the comment
     * THEIR revision alone changed is taken, the way a sheet is merged cell by cell. Where both revisions changed a
     * part differently, OUR revision keeps it, since no table depends on it.
     *
     * @param cells the cells of OUR revision that change; a cell the tables write is left to them
     */
    private void looseCells(Map<CellAddress, CellSource> cells) {
        var places = new Places(Stream.of(tables, layout, clearedTables).flatMap(List::stream));
        var addresses = new HashSet<CellAddress>();
        for (Cursor side : List.of(base, their)) {
            side.sheet.forEach(row -> row.forEach(cell -> addresses.add(cell.getAddress())));
        }
        for (CellAddress address : addresses) {
            if (!cells.containsKey(address) && !places.hold(address)) {
                var parts = looseParts(address);
                if (parts != null && !parts.isEmpty()) {
                    cells.put(address, new CellSource(address, address, parts));
                }
            }
        }
    }

    /**
     * The places of tables, found by the rows they span: the tables of the three revisions, and the places the merge
     * writes tables at. A cell outside all of them is merged cell by cell.
     */
    private static final class Places {

        private final Map<Integer, List<CellRangeAddress>> byRow = new HashMap<>();

        Places(Stream<CellRangeAddress> places) {
            places.forEach(place -> {
                for (var row = place.getFirstRow(); row <= place.getLastRow(); row++) {
                    byRow.computeIfAbsent(row, key -> new ArrayList<>()).add(place);
                }
            });
        }

        /** Tells whether a place holds the cell. */
        boolean hold(CellAddress address) {
            return byRow.getOrDefault(address.getRow(), List.of()).stream().anyMatch(place -> place.isInRange(address));
        }
    }

    /**
     * The parts of a cell outside every table that THEIR revision alone changed.
     *
     * @return the parts, or {@code null} if both revisions changed a part differently
     */
    private @Nullable Set<CellPart> looseParts(CellAddress address) {
        base.cell = SheetUtil.getCell(base.sheet, address.getRow(), address.getColumn());
        our.cell = SheetUtil.getCell(our.sheet, address.getRow(), address.getColumn());
        their.cell = SheetUtil.getCell(their.sheet, address.getRow(), address.getColumn());
        try {
            return XlsSheetMerger.theirParts(base, our, their);
        } finally {
            base.cell = our.cell = their.cell = null;
        }
    }

    /**
     * Merges one table of BASE revision with its revisions.
     *
     * @return why the changes of the table conflict, or {@code null} when they are merged
     */
    private @Nullable TableConflictReason mergeTable(CellRangeAddress baseTable,
                                                     @Nullable CellRangeAddress ourTable,
                                                     @Nullable CellRangeAddress theirTable) {
        var baseView = view(base, baseTable);
        if (ourTable == null || theirTable == null) {
            return mergeRemoved(baseView, ourTable, theirTable);
        }
        var ourView = view(our, ourTable);
        var theirView = view(their, theirTable);
        var start = mergedStart(baseTable, ourTable, theirTable);
        if (start == null) {
            return TableConflictReason.MOVED;
        }
        if (equalTables(baseView, theirView) || equalTables(ourView, theirView)) {
            return takeOurs(ourTable, theirTable, start);
        } else if (equalTables(baseView, ourView)) {
            return writeWhole(ourTable, Side.THEIR, theirView, ourTable, place(start, theirTable));
        }
        var reason = mergeLines(baseTable, ourTable, theirTable, start);
        // A revision that changed only the formatting of the table does not keep its lines from being taken whole.
        if (reason != null && holdAlike(baseView, theirView)) {
            return takeOurs(ourTable, theirTable, start);
        } else if (reason != null && holdAlike(baseView, ourView)) {
            return writeWhole(ourTable, Side.THEIR, theirView, ourTable, place(start, theirTable));
        }
        return reason;
    }

    /**
     * Merges a table that one or both revisions removed: a removed table stays removed when the other revision left
     * its content as it was, and conflicts otherwise.
     *
     * @return why the changes of the table conflict, or {@code null} when they are merged
     */
    private @Nullable TableConflictReason mergeRemoved(View baseView,
                                                       @Nullable CellRangeAddress ourTable,
                                                       @Nullable CellRangeAddress theirTable) {
        if (ourTable != null) {
            if (!holdAlike(baseView, view(our, ourTable))) {
                return TableConflictReason.REMOVED_AND_CHANGED;
            }
            clearedTables.add(ourTable);
            return null;
        }
        return theirTable == null || holdAlike(baseView, view(their, theirTable))
                ? null
                : TableConflictReason.REMOVED_AND_CHANGED;
    }

    /**
     * Merges a table changed in both revisions line by line: by rows, or by columns when a revision inserted or
     * deleted columns. Only one of the two can be merged at a time.
     *
     * @return why the changes of the table conflict, or {@code null} when they are merged
     */
    private @Nullable TableConflictReason mergeLines(CellRangeAddress baseTable,
                                                     CellRangeAddress ourTable,
                                                     CellRangeAddress theirTable,
                                                     CellAddress start) {
        var rows = new Views(view(base, baseTable), view(our, ourTable), view(their, theirTable));
        var columns = new Views(new View(base, baseTable, true),
                new View(our, ourTable, true),
                new View(their, theirTable, true));
        var byRows = rowAlignments(baseTable, rows);
        var byColumns = alignments(columns);
        if (byRows == null || byColumns == null || byRows.isStructural() && byColumns.isStructural()) {
            return TableConflictReason.ROWS_AND_COLUMNS;
        }
        var transposed = byColumns.isStructural();
        var alignments = transposed ? byColumns : byRows;
        var views = transposed ? columns : rows;
        var positions = views.base().positions();
        if (positions != views.our().positions() || positions != views.their().positions()) {
            return TableConflictReason.ROWS_AND_COLUMNS;
        }
        var lines = new LineMerger(views.base(), views.our(), views.their(), alignments).merge();
        if (lines == null) {
            return TableConflictReason.ROWS;
        }
        var target = transposed
                ? place(start, positions, lines.lines().size())
                : place(start, lines.lines().size(), positions);
        var output = new TableOutput(views, lines, target);
        var id = idOf(baseTable);
        var byCells = conflicts.decidedByCells(id);
        var reason = byCells == null && !conflicts.surveys() ? output.build() : output.buildByCells(id, byCells);
        if (output.merged != null) {
            mergedByCells.put(baseTable, output.merged);
        }
        if (reason == null) {
            keep(ourTable, output);
        }
        return reason;
    }

    /**
     * Aligns the lines of BASE revision of a table with OUR and THEIR revisions.
     *
     * @return the alignments, or {@code null} when too many lines changed to align them
     */
    private static @Nullable Alignments alignments(Views views) {
        var base = Objects.requireNonNull(views.base());
        var baseLines = lineDigests(base.cellDigests());
        var baseShown = base.shownDigests();
        var ourAlignment = align(baseLines, baseShown, Objects.requireNonNull(views.our()));
        var theirAlignment = align(baseLines, baseShown, Objects.requireNonNull(views.their()));
        return ourAlignment == null || theirAlignment == null ? null : new Alignments(ourAlignment, theirAlignment);
    }

    /**
     * The rows of a table both revisions kept aligned with BASE revision, aligned once for the table.
     *
     * @param rows the table in the three revisions, seen by its rows
     */
    private @Nullable Alignments rowAlignments(CellRangeAddress baseTable, Views rows) {
        return rowAlignments.computeIfAbsent(baseTable, table -> Optional.ofNullable(alignments(rows))).orElse(null);
    }

    /**
     * Aligns the lines of BASE revision with the lines of another revision. Lines are told apart by the values of
     * their cells, and lines changed in place are told by the values their cells show, merged cells included.
     */
    private static @Nullable LineAlignment align(long[] baseLines, long[][] baseShown, View side) {
        return LineAlignment.align(baseLines,
                lineDigests(side.cellDigests()),
                similarity(baseShown, side.shownDigests()));
    }

    /**
     * Digests of lines from the digests of their cells, which tell lines apart.
     */
    private static long[] lineDigests(long[][] cells) {
        var digests = new long[cells.length];
        for (var line = 0; line < cells.length; line++) {
            digests[line] = lineDigest(cells[line]);
        }
        return digests;
    }

    /**
     * The digest of a line from the digests of its cells.
     */
    private static long lineDigest(long[] cells) {
        var digest = 0L;
        for (var position = 0; position < cells.length; position++) {
            digest += XlsSheetShifts.mix(position, cells[position]);
        }
        return digest;
    }

    /**
     * Tells how alike the lines of two tables are: the number of values they share at the same positions, when they
     * share at least half of their values.
     *
     * @param cells      the digests of the values the cells of one table show, by line and position
     * @param otherCells the digests of the values the cells of the other table show, by line and position
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
     * content moves when the table changes its size or its place. A table taken whole from BASE revision moves the
     * content of OUR revision the same way.
     *
     * @param other the table in the other revision, or {@code null} for a table the source revision created
     * @return why the table cannot be written, or {@code null} when it is
     */
    private @Nullable TableConflictReason writeWhole(@Nullable CellRangeAddress ourTable,
                                                     Side side,
                                                     View source,
                                                     @Nullable CellRangeAddress other,
                                                     CellRangeAddress target) {
        var views = switch (side) {
            case BASE -> new Views(source, null, null);
            case OUR -> new Views(null, source, null);
            case THEIR -> new Views(null, null, source);
        };
        var reason = write(ourTable, views, Lines.whole(side, source.lines()), target);
        if (reason == null) {
            otherMoved(side, other, target);
        }
        return reason;
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
     * @return why the table cannot be written: the cells of a line conflict, or a change crosses the merged cells of
     * the table; {@code null} when it is written
     */
    private @Nullable TableConflictReason write(@Nullable CellRangeAddress ourTable,
                                                Views views,
                                                Lines lines,
                                                CellRangeAddress target) {
        var output = new TableOutput(views, lines, target);
        var reason = output.build();
        if (reason == null) {
            keep(ourTable, output);
        }
        return reason;
    }

    /**
     * Keeps a merged table built at its place, clearing the place of the table in OUR revision first.
     */
    private void keep(@Nullable CellRangeAddress ourTable, TableOutput output) {
        if (ourTable != null) {
            clearedTables.add(ourTable);
        }
        layout.add(output.target);
        outputs.add(output);
        ourMoved.addAll(output.moved(Side.OUR));
        theirMoved.addAll(output.moved(Side.THEIR));
        baseMoved.addAll(output.moved(Side.BASE));
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
     * <p>The merged sheet keeps the data validation, the hyperlinks and the defined names of OUR revision, which THEIR
     * revision has as in BASE revision or as OUR revision has them, and the conditional formatting of OUR revision,
     * since content moves. It keeps the formulas of OUR revision, and a
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
     * Tells whether a formula the merge takes from BASE revision refers to content that lands at another address, or
     * stands in such content itself. A formula keeps the addresses BASE revision has, while a revision may have moved,
     * resized or removed what it refers to.
     */
    private boolean baseRefersToMoved() {
        var taken = outputs.stream()
                .flatMap(output -> output.cells.values().stream())
                .map(CellSource::base)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (taken.isEmpty()) {
            return false;
        }
        var sheetName = base.sheet.getSheetName();
        Predicate<Cell> kept = cell -> cell.getSheet().getSheetName().equals(sheetName)
                && taken.contains(cell.getAddress());
        return XlsFormulaReferences.refersTo(base.originalWorkbook(), sheetName, baseMoved, kept);
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
        return sameTables(a, b, View::equalLine);
    }

    /**
     * Tells whether two tables hold the same values, comments and merged cells, whatever their formatting.
     */
    private static boolean holdAlike(View a, View b) {
        return sameTables(a, b, View::holdsLine);
    }

    private static boolean sameTables(View a, View b, SameLine sameLine) {
        if (a.lines() != b.lines() || a.positions() != b.positions()) {
            return false;
        }
        for (var line = 0; line < a.lines(); line++) {
            if (!sameLine.test(a, line, b, line)) {
                return false;
            }
        }
        return a.regions().equals(b.regions());
    }

    /** Compares a line of a table with a line of another table. */
    @FunctionalInterface
    private interface SameLine {

        boolean test(View view, int line, View other, int otherLine);
    }

    private static CellAddress start(CellRangeAddress table) {
        return new CellAddress(table.getFirstRow(), table.getFirstColumn());
    }

    private static CellRangeAddress place(CellAddress start, CellRangeAddress table) {
        return place(start, height(table), width(table));
    }

    private static int height(CellRangeAddress table) {
        return table.getLastRow() - table.getFirstRow() + 1;
    }

    private static int width(CellRangeAddress table) {
        return table.getLastColumn() - table.getFirstColumn() + 1;
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
     * Tells whether a cell holds the name of the properties of a table.
     */
    private static boolean namesProperties(@Nullable Cell cell) {
        return cell != null && cell.getCellType() == CellType.STRING
                && PROPERTIES.equals(cell.getStringCellValue().strip());
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
     * The rows one revision, the pusher, inserted or deleted above its tables, carried over onto the places the other
     * revision, the layout, gives the tables.
     *
     * <p>A revision only pushes its tables when it moves none of them across another or to another column: it grows or
     * shrinks tables, or adds or removes rows between them. The merged sheet then follows the layout, each table moved
     * down or up by the rows the pusher added above it: the rows a table above grew or shrank by, less those the layout
     * inserted or deleted alike, and the rows added or removed between tables where the layout kept the rows between
     * them as BASE revision has them. A table only the pusher created keeps its place, which the pusher has pushed
     * already.
     *
     * <p>THEIR revision is the pusher when it only pushes its tables, and OUR revision otherwise. A table both
     * revisions moved on purpose stays a conflict: a revision moves a table on purpose when it puts the table into
     * another column, or moves it by rows that no table above it, or to its left on its row, explains. A table beside
     * another, on its rows, moves with the rows both revisions inserted alike into that table above it.
     *
     * @param steps       the rows the pusher added below a row of the layout, by the last row of the table they
     *                    follow; {@code -1} for the rows above every table
     * @param movedByBoth the tables of BASE revision both revisions moved on purpose
     */
    private record Pushes(Side layout, List<Step> steps, Set<CellRangeAddress> movedByBoth) {

        /**
         * @param shared the rows both revisions inserted alike into a table of BASE revision above one of its rows
         * @return the pushes, or {@code null} when neither revision only pushes its tables
         */
        static @Nullable Pushes of(List<CellRangeAddress> baseTables,
                                   Map<CellRangeAddress, CellRangeAddress> ourPairs,
                                   Map<CellRangeAddress, CellRangeAddress> theirPairs,
                                   SharedRows shared) {
            var movedByBoth = movedOnPurpose(baseTables, ourPairs, shared);
            movedByBoth.retainAll(movedOnPurpose(baseTables, theirPairs, shared));
            var theirSteps = steps(baseTables, theirPairs, ourPairs, shared);
            if (theirSteps != null) {
                return new Pushes(Side.OUR, theirSteps, movedByBoth);
            }
            var ourSteps = steps(baseTables, ourPairs, theirPairs, shared);
            return ourSteps == null ? null : new Pushes(Side.THEIR, ourSteps, movedByBoth);
        }

        /**
         * The tables a revision moved on purpose: into another column, or by rows no table before it explains. A table
         * before it starts above it, or on its row to its left.
         */
        private static Set<CellRangeAddress> movedOnPurpose(List<CellRangeAddress> baseTables,
                                                            Map<CellRangeAddress, CellRangeAddress> pairs,
                                                            SharedRows shared) {
            var kept = kept(baseTables, pairs);
            var moved = new HashSet<CellRangeAddress>();
            for (var i = 0; i < kept.size(); i++) {
                var table = kept.get(i);
                // the tables before it from top to bottom, and from left to right on its row
                var pushed = shift(table, pairs) == 0 || kept.subList(0, i)
                        .stream()
                        .anyMatch(other -> pushedBy(table, other, pairs, shared));
                if (pairs.get(table).getFirstColumn() != table.getFirstColumn() || !pushed) {
                    moved.add(table);
                }
            }
            return moved;
        }

        /**
         * Tells whether a table before it explains the rows a table moved by: that table moved as far, or as far once
         * grown or shrunk.
         *
         * <p>A table beside it, on its rows, moves by the rows both revisions inserted alike into it above its first
         * row too. Rows only one revision inserted there leave the place of the table to that revision.
         */
        private static boolean pushedBy(CellRangeAddress table,
                                        CellRangeAddress other,
                                        Map<CellRangeAddress, CellRangeAddress> pairs,
                                        SharedRows shared) {
            var shift = shift(table, pairs);
            var otherShift = shift(other, pairs);
            return shift == otherShift || shift == otherShift + growth(other, pairs)
                    || table.getFirstRow() <= other.getLastRow()
                    && shift == otherShift + shared.above(other, table.getFirstRow() - other.getFirstRow());
        }

        /**
         * The tables of BASE revision a revision keeps, from top to bottom and from left to right.
         *
         * @param baseTables the tables of BASE revision, in that order
         */
        private static List<CellRangeAddress> kept(List<CellRangeAddress> baseTables,
                                                   Map<CellRangeAddress, CellRangeAddress> pairs) {
            return baseTables.stream().filter(table -> pairs.get(table) != null).toList();
        }

        private static int shift(CellRangeAddress table, Map<CellRangeAddress, CellRangeAddress> pairs) {
            return pairs.get(table).getFirstRow() - table.getFirstRow();
        }

        /**
         * The rows a revision grew a table by, or shrank it by when negative.
         */
        private static int growth(CellRangeAddress table, Map<CellRangeAddress, CellRangeAddress> pairs) {
            return height(pairs.get(table)) - height(table);
        }

        /**
         * The rows a pusher added below each table, as the layout puts the table. The rows a table grew or shrank by
         * count less those the layout inserted or deleted alike, which the layout has already.
         *
         * @return the steps, or {@code null} when the revision moved a table across another or to another column, or
         * pushed the tables under one the layout removed
         */
        private static @Nullable List<Step> steps(List<CellRangeAddress> baseTables,
                                                  Map<CellRangeAddress, CellRangeAddress> pusher,
                                                  Map<CellRangeAddress, CellRangeAddress> layout,
                                                  SharedRows shared) {
            var kept = kept(baseTables, pusher);
            if (!pushesOnly(kept, pusher)) {
                return null;
            }
            var steps = new ArrayList<Step>();
            for (var i = 0; i < kept.size(); i++) {
                var previous = i == 0 ? null : kept.get(i - 1);
                var rows = rowsAdded(previous, kept.get(i), pusher, layout, shared);
                var anchor = previous == null ? null : layout.get(previous);
                if (rows != 0 && previous != null && anchor == null) {
                    return null;
                } else if (rows != 0) {
                    steps.add(new Step(anchor == null ? -1 : anchor.getLastRow(), rows));
                }
            }
            return steps;
        }

        /**
         * The rows a pusher added above a table, below the table above it: the rows that table grew by, less those the
         * layout inserted alike into it, and the rows inserted between the two when the layout keeps the rows between
         * them as BASE revision has them.
         *
         * @param previous the table above, or {@code null} for the rows above every table
         */
        private static int rowsAdded(@Nullable CellRangeAddress previous,
                                     CellRangeAddress table,
                                     Map<CellRangeAddress, CellRangeAddress> pusher,
                                     Map<CellRangeAddress, CellRangeAddress> layout,
                                     SharedRows shared) {
            if (previous == null) {
                return keepsRowsBetween(layout, null, table) ? shift(table, pusher) : 0;
            }
            var growth = growth(previous, pusher);
            var added = growth == 0 ? 0 : growth - shared.above(previous, height(previous));
            var between = shift(table, pusher) - shift(previous, pusher) - growth;
            return added + (keepsRowsBetween(layout, previous, table) ? between : 0);
        }

        /**
         * Tells whether a revision keeps every table in its column and in its order from top to bottom.
         */
        private static boolean pushesOnly(List<CellRangeAddress> kept, Map<CellRangeAddress, CellRangeAddress> pusher) {
            return kept.stream().allMatch(table -> pusher.get(table).getFirstColumn() == table.getFirstColumn())
                    && IntStream.range(1, kept.size())
                    .allMatch(i -> Integer.signum(kept.get(i).getFirstRow() - kept.get(i - 1).getFirstRow())
                            == Integer.signum(pusher.get(kept.get(i)).getFirstRow()
                            - pusher.get(kept.get(i - 1)).getFirstRow()));
        }

        /**
         * Tells whether the layout keeps the rows between a table and the one above it as BASE revision has them.
         *
         * @param previous the table above, or {@code null} for the rows above every table
         */
        private static boolean keepsRowsBetween(Map<CellRangeAddress, CellRangeAddress> layout,
                                                @Nullable CellRangeAddress previous,
                                                CellRangeAddress table) {
            var laid = layout.get(table);
            if (laid == null) {
                return false;
            } else if (previous == null) {
                return laid.getFirstRow() == table.getFirstRow();
            }
            var laidPrevious = layout.get(previous);
            return laidPrevious != null
                    && laid.getFirstRow() - laidPrevious.getLastRow() == table.getFirstRow() - previous.getLastRow();
        }

        /**
         * The rows the pusher added above a row of the layout.
         */
        private int rowsAbove(int row) {
            return steps.stream().filter(step -> step.anchor() < row).mapToInt(Step::rows).sum();
        }

        /**
         * Where a table of the layout lands.
         *
         * @return the top left cell, or {@code null} when the rows taken off would take the table off the sheet
         */
        @Nullable CellAddress start(CellRangeAddress laidTable) {
            var row = laidTable.getFirstRow() + rowsAbove(laidTable.getFirstRow());
            return row < 0 ? null : new CellAddress(row, laidTable.getFirstColumn());
        }

        /**
         * Where a table only the layout created lands.
         */
        CellRangeAddress pushed(CellRangeAddress laidTable) {
            var start = start(laidTable);
            return start == null ? laidTable : place(start, laidTable);
        }
    }

    /**
     * The rows both revisions inserted alike into a table of BASE revision above one of its rows, less the rows both
     * deleted alike there.
     */
    @FunctionalInterface
    private interface SharedRows {

        /**
         * @param line the row of the table in BASE revision, counted from its first row
         */
        int above(CellRangeAddress baseTable, int line);
    }

    /**
     * Rows a revision added below the given row of the layout, or removed when negative.
     *
     * @param anchor the last row of the table the rows follow in the layout, or {@code -1} for the rows above every
     *               table
     */
    private record Step(int anchor, int rows) {
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
            return height(table);
        }

        private int columns() {
            return width(table);
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
            var digests = new long[lines()][];
            for (var line = 0; line < digests.length; line++) {
                digests[line] = cellDigests(line);
            }
            return digests;
        }

        /**
         * Digests of the values the cells show by line and position, as OpenL reads them: a cell that merged cells
         * cover shows the value of their first cell.
         *
         * <p>A property a revision adds above the properties of a table stretches the properties cell over the row of
         * the property that was there. That row still shows the same name of the properties, so it is still found as
         * that row, not as the row added.
         */
        long[][] shownDigests() {
            var digests = cellDigests();
            cursor.sheet.getMergedRegions()
                    .stream()
                    .filter(region -> region.intersects(table))
                    .forEach(region -> showFirstCell(digests, region));
            return digests;
        }

        /**
         * Gives the cells of this table that merged cells cover the digest of the first of the merged cells, which is
         * the value they show whatever value a covered cell keeps.
         */
        private void showFirstCell(long[][] digests, CellRangeAddress region) {
            var first = SheetUtil.getCell(cursor.sheet, region.getFirstRow(), region.getFirstColumn());
            var shown = first == null ? 0 : XlsSheetShifts.digest(first);
            var lastRow = Math.min(region.getLastRow(), table.getLastRow());
            var lastColumn = Math.min(region.getLastColumn(), table.getLastColumn());
            for (var row = Math.max(region.getFirstRow(), table.getFirstRow()); row <= lastRow; row++) {
                for (var column = Math.max(region.getFirstColumn(), table.getFirstColumn()); column <= lastColumn;
                     column++) {
                    if (row != region.getFirstRow() || column != region.getFirstColumn()) {
                        var line = transposed ? column - table.getFirstColumn() : row - table.getFirstRow();
                        var position = transposed ? row - table.getFirstRow() : column - table.getFirstColumn();
                        digests[line][position] = shown;
                    }
                }
            }
        }

        /**
         * Tells whether a row of the table is a property of it: its first cell shows the name of the properties, by
         * itself or as the first cell of merged cells that cover it.
         */
        boolean showsProperties(int line) {
            if (transposed || line == 0) {
                return false;
            }
            var address = address(line, 0);
            var region = cursor.getMergedRegion(address.getRow(), address.getColumn());
            var cell = region == null
                    ? cell(line, 0)
                    : SheetUtil.getCell(cursor.sheet, region.getFirstRow(), region.getFirstColumn());
            return namesProperties(cell);
        }

        /**
         * Digests of the values of the cells of a line by position, {@code 0} for a cell with nothing in it.
         */
        long[] cellDigests(int line) {
            var digests = new long[positions()];
            for (var position = 0; position < digests.length; position++) {
                var cell = cell(line, position);
                digests[position] = cell == null ? 0 : XlsSheetShifts.digest(cell);
            }
            return digests;
        }

        /**
         * Tells whether a line of this table and a line of another table have the same cells: values, styles and
         * comments.
         */
        boolean equalLine(int line, View other, int otherLine) {
            return sameCells(line, other, otherLine, true, 0);
        }

        /**
         * Tells whether a line of this table and a line of another table hold the same: values and comments, whatever
         * their formatting.
         */
        boolean holdsLine(int line, View other, int otherLine) {
            return sameCells(line, other, otherLine, false, 0);
        }

        /**
         * Tells whether a property of this table and a property of another table hold the same name and value,
         * whatever their first cells, of which only the first property shows the properties cell.
         */
        boolean holdsProperty(int line, View other, int otherLine) {
            return sameCells(line, other, otherLine, false, 1);
        }

        private boolean sameCells(int line, View other, int otherLine, boolean formatting, int from) {
            for (var position = from; position < positions(); position++) {
                cursor.cell = cell(line, position);
                other.cursor.cell = other.cell(otherLine, position);
                var equal = XlsSheetsMatcher.equalValues(cursor, other.cursor)
                        && (!formatting || XlsSheetsMatcher.equalStyles(cursor, other.cursor))
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
     * @param length    the number of lines the merged cells cover in the revision, inserted lines included, less those
     *                  inserted where both revisions inserted lines
     * @param joints    the places both revisions inserted lines at that the merged cells stretch over
     */
    private record RegionKey(int firstLine,
                             int lastLine,
                             int firstPosition,
                             int lastPosition,
                             int length,
                             Set<Joint> joints) {
    }

    /**
     * The tables of a merge seen the same way. A revision that gives no lines to the merged table has no view.
     */
    private record Views(@Nullable View base, @Nullable View our, @Nullable View their) {

        /**
         * The view of OUR revision, or of THEIR revision when OUR revision gives no lines, or of BASE revision for a
         * table taken whole from it.
         */
        View any() {
            if (our != null) {
                return our;
            }
            return Objects.requireNonNull(their != null ? their : base);
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
     *
     * <p>A line every revision has is merged cell by cell. Any other line is taken whole from one revision: both OUR
     * and THEIR revisions keep a line of BASE revision that neither of them deleted. A line both revisions inserted
     * alike at one place is taken from OUR revision.
     */
    private record Line(int base, int our, int their) {

        /**
         * A line taken whole from one revision.
         */
        static Line of(Side side, int line) {
            return switch (side) {
                case BASE -> new Line(line, -1, -1);
                case OUR -> new Line(-1, line, -1);
                case THEIR -> new Line(-1, -1, line);
            };
        }

        /**
         * Tells whether every revision has the line, so its cells are merged.
         */
        boolean merged() {
            return base >= 0 && our >= 0 && their >= 0;
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
     * @param joints     the places both revisions inserted lines at
     */
    private record Lines(List<Line> lines, @Nullable Alignments alignments, List<Joint> joints) {

        /**
         * The rows of a table taken whole from one revision.
         */
        static Lines whole(Side side, int count) {
            var lines = new ArrayList<Line>(count);
            for (var i = 0; i < count; i++) {
                lines.add(Line.of(side, i));
            }
            return new Lines(lines, null, List.of());
        }

        /**
         * The place both revisions inserted a line of the merged table at, or {@code null} for a line of another
         * place.
         */
        @Nullable Joint jointOf(int line) {
            return joints.stream().filter(joint -> joint.from() <= line && line < joint.to()).findFirst().orElse(null);
        }
    }

    /**
     * A place both revisions inserted lines at: the lines of OUR revision come first, and then the lines of THEIR
     * revision that OUR revision has not inserted alike.
     *
     * @param base the line of BASE revision the inserted lines stand before
     * @param from the first line of the merged table inserted there
     * @param to   the line of the merged table after the last one inserted there
     */
    private record Joint(int base, int from, int to) {
    }

    /**
     * Merges the lines of a table changed in both revisions, the way a three-way merge of text merges lines.
     *
     * <p>The lines both revisions inserted at the same place follow one another, those of OUR revision first. A line
     * THEIR revision inserted there that OUR revision inserted alike is not repeated, and neither are the lines of a
     * structural change both revisions made alike: a line of OUR revision stands for the line of THEIR revision.
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class LineMerger {

        private final View base;
        private final View our;
        private final View their;
        private final Alignments alignments;
        /**
         * The lines of OUR revision alike the lines THEIR revision inserted, by the insertion of THEIR revision.
         */
        private final Map<LineAlignment.Hunk, int[]> alikeLines = new HashMap<>();

        /**
         * @return the lines of the merged table, or {@code null} if the changes conflict
         */
        @Nullable Lines merge() {
            var ourHunks = alignments.our().hunks();
            var theirHunks = new ArrayList<LineAlignment.Hunk>();
            var madeAlike = new ArrayList<LineAlignment.Hunk>();
            for (LineAlignment.Hunk hunk : alignments.their().hunks()) {
                if (ourHunks.stream().anyMatch(ourHunk -> sameHunk(ourHunk, hunk))) {
                    madeAlike.add(hunk);
                } else if (ourHunks.stream().anyMatch(ourHunk -> overlap(ourHunk, hunk) && !joint(ourHunk, hunk))) {
                    return null;
                } else {
                    theirHunks.add(hunk);
                }
            }
            if (changedUnder(ourHunks, their, alignments.their()) || changedUnder(theirHunks, our, alignments.our())) {
                return null;
            }
            return lines(ourHunks, theirHunks, madeAlike);
        }

        /**
         * @param madeAlike the structural changes of THEIR revision that OUR revision made alike
         */
        private Lines lines(List<LineAlignment.Hunk> ourHunks,
                            List<LineAlignment.Hunk> theirHunks,
                            List<LineAlignment.Hunk> madeAlike) {
            var lines = new ArrayList<Line>();
            var joints = new ArrayList<Joint>();
            var covered = new boolean[base.lines()];
            for (var i = 0; i <= base.lines(); i++) {
                var ourHunk = hunkAt(i, ourHunks);
                var theirHunk = hunkAt(i, theirHunks);
                var alikeHunk = hunkAt(i, madeAlike);
                var from = lines.size();
                addLines(ourHunk, Side.OUR, lines, covered);
                if (ourHunk != null && alikeHunk != null) {
                    for (var line = 0; line < alikeHunk.sideTo() - alikeHunk.sideFrom(); line++) {
                        lines.set(from + line, new Line(-1, ourHunk.sideFrom() + line, alikeHunk.sideFrom() + line));
                    }
                } else if (ourHunk != null && theirHunk != null) {
                    addInsertedAlongside(ourHunk, theirHunk, from, lines);
                    joints.add(new Joint(i, from, lines.size()));
                } else {
                    addLines(theirHunk, Side.THEIR, lines, covered);
                }
                if (i < base.lines() && !covered[i]) {
                    lines.add(new Line(i, alignments.our().sideOf()[i], alignments.their().sideOf()[i]));
                }
            }
            return new Lines(lines, alignments, joints);
        }

        /**
         * The structural change a revision made before a line of BASE revision, if any.
         */
        private static LineAlignment.@Nullable Hunk hunkAt(int baseLine, List<LineAlignment.Hunk> hunks) {
            return hunks.stream().filter(hunk -> hunk.baseFrom() == baseLine).findFirst().orElse(null);
        }

        /**
         * Adds the lines of a structural change of a revision, and marks the lines of BASE revision they replace.
         */
        private static void addLines(LineAlignment.@Nullable Hunk hunk, Side side, List<Line> lines, boolean[] covered) {
            if (hunk != null) {
                for (var line = hunk.sideFrom(); line < hunk.sideTo(); line++) {
                    lines.add(Line.of(side, line));
                }
                Arrays.fill(covered, hunk.baseFrom(), hunk.baseTo(), true);
            }
        }

        /**
         * Adds the lines THEIR revision inserted where OUR revision inserted lines too, after those of OUR revision.
         * A line OUR revision inserted alike stands for the line of THEIR revision.
         *
         * @param from the line of the merged table the lines OUR revision inserted there start at
         */
        private void addInsertedAlongside(LineAlignment.Hunk ourHunk,
                                          LineAlignment.Hunk theirHunk,
                                          int from,
                                          List<Line> lines) {
            var alike = alike(ourHunk, theirHunk);
            for (var i = 0; i < alike.length; i++) {
                var theirLine = theirHunk.sideFrom() + i;
                if (alike[i] < 0) {
                    lines.add(Line.of(Side.THEIR, theirLine));
                } else {
                    lines.set(from + alike[i] - ourHunk.sideFrom(), new Line(-1, alike[i], theirLine));
                }
            }
        }

        /**
         * Finds the lines OUR revision inserted alike for the lines THEIR revision inserted at the same place: lines
         * holding the same values and comments, each line of OUR revision standing for one line of THEIR revision.
         *
         * @return the line of OUR revision by the line THEIR revision inserted, counted from the first one, or
         * {@code -1} for a line OUR revision has not inserted alike
         */
        private int[] alike(LineAlignment.Hunk ourHunk, LineAlignment.Hunk theirHunk) {
            return alikeLines.computeIfAbsent(theirHunk, hunk -> findAlike(ourHunk, hunk));
        }

        private int[] findAlike(LineAlignment.Hunk ourHunk, LineAlignment.Hunk theirHunk) {
            var ourLines = new HashMap<Long, List<Integer>>();
            for (var line = ourHunk.sideFrom(); line < ourHunk.sideTo(); line++) {
                ourLines.computeIfAbsent(alikeDigest(our, line), digest -> new ArrayList<>()).add(line);
            }
            var alike = new int[theirHunk.sideTo() - theirHunk.sideFrom()];
            for (var i = 0; i < alike.length; i++) {
                var theirLine = theirHunk.sideFrom() + i;
                alike[i] = takeAlike(ourLines.getOrDefault(alikeDigest(their, theirLine), List.of()), theirLine);
            }
            return alike;
        }

        /**
         * The digest a line inserted is told alike by: its values, or the name and the value of a property, whose
         * first cell is the properties cell only one of the properties shows.
         */
        private static long alikeDigest(View view, int line) {
            var digests = view.cellDigests(line);
            if (view.showsProperties(line)) {
                digests[0] = 0;
            }
            return lineDigest(digests);
        }

        /**
         * Takes the first line of OUR revision that holds the same as a line of THEIR revision out of the candidates.
         *
         * @return the line, or {@code -1} when none holds the same
         */
        private int takeAlike(List<Integer> candidates, int theirLine) {
            for (var iterator = candidates.iterator(); iterator.hasNext(); ) {
                var ourLine = iterator.next();
                var property = our.showsProperties(ourLine) && their.showsProperties(theirLine);
                if (property ? our.holdsProperty(ourLine, their, theirLine) : our.holdsLine(ourLine, their, theirLine)) {
                    iterator.remove();
                    return ourLine;
                }
            }
            return -1;
        }

        /**
         * The lines both revisions inserted alike before a line of BASE revision, less the lines both deleted alike
         * there.
         */
        int sharedLines(int line) {
            var theirHunks = alignments.their().hunks();
            return alignments.our()
                    .hunks()
                    .stream()
                    .filter(hunk -> hunk.baseTo() <= line)
                    .mapToInt(hunk -> theirHunks.stream().mapToInt(theirHunk -> sharedLines(hunk, theirHunk)).sum())
                    .sum();
        }

        /**
         * The lines two structural changes of the revisions share: the lines a change both revisions made alike
         * inserts less those it deletes, or the lines both revisions inserted alike at the same place.
         */
        private int sharedLines(LineAlignment.Hunk ourHunk, LineAlignment.Hunk theirHunk) {
            if (sameHunk(ourHunk, theirHunk)) {
                return ourHunk.sideTo() - ourHunk.sideFrom() - ourHunk.baseTo() + ourHunk.baseFrom();
            } else if (joint(ourHunk, theirHunk)) {
                return (int) Arrays.stream(alike(ourHunk, theirHunk)).filter(line -> line >= 0).count();
            }
            return 0;
        }

        /**
         * Tells whether both revisions only inserted lines at the same place, which the merge keeps one after another.
         *
         * <p>Columns inserted before the first column of a table are not kept so, since that column holds the header
         * of the table. Neither are rows inserted right under the header, where a table keeps its properties, unless
         * every one of them is a property: the properties of both revisions are then joined under one properties
         * cell.
         */
        private boolean joint(LineAlignment.Hunk ourHunk, LineAlignment.Hunk theirHunk) {
            return ourHunk.baseFrom() == ourHunk.baseTo()
                    && theirHunk.baseFrom() == theirHunk.baseTo()
                    && ourHunk.baseFrom() == theirHunk.baseFrom()
                    && (ourHunk.baseFrom() > (base.transposed() ? 0 : 1)
                    || ourHunk.baseFrom() == 1 && properties(ourHunk, our) && properties(theirHunk, their));
        }

        /**
         * Tells whether every line a structural change of a revision inserted is a property of the table.
         */
        private static boolean properties(LineAlignment.Hunk hunk, View side) {
            return IntStream.range(hunk.sideFrom(), hunk.sideTo()).allMatch(side::showsProperties);
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
                if (!our.holdsLine(ourHunk.sideFrom() + i, their, theirHunk.sideFrom() + i)) {
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
                    if (otherLine >= 0 && !base.holdsLine(line, other, otherLine)) {
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
        /**
         * The cells both revisions changed differently, when they are collected rather than leave the table a
         * conflict.
         */
        private final List<CellConflict> conflictCells = new ArrayList<>();
        /**
         * The cells whose value or comment one revision alone changed, collected with the cells both revisions changed
         * differently.
         */
        private final Set<CellAddress> changedCells = new HashSet<>();
        /**
         * The table merged cell by cell for a survey, or {@code null} when it was not, or cannot be.
         */
        private @Nullable TableCells merged;
        /**
         * Whether the cells both revisions changed differently are collected, each with OUR version of what conflicts,
         * rather than leave the table a conflict.
         */
        private boolean collecting;
        /**
         * Whether the cells one revision alone changed are noted, which only a survey lists.
         */
        private boolean notingChanges;

        private TableOutput(Views views, Lines lines, CellRangeAddress target) {
            this.views = views;
            this.lines = lines;
            this.target = target;
            this.transposed = views.any().transposed();
        }

        /**
         * @return why the table cannot be built: the cells of a line conflict, or a change crosses merged cells;
         * {@code null} when it is built
         */
        @Nullable TableConflictReason build() {
            if (!buildLines()) {
                return TableConflictReason.CELLS;
            } else if (repeatsProperty()) {
                return TableConflictReason.ROWS;
            }
            renumberColumns();
            return buildRegions() ? null : TableConflictReason.MERGED_CELLS;
        }

        /**
         * Builds the table cell by cell: the cells both revisions changed differently are collected, and then taken as
         * a resolution decides, or listed for a survey.
         *
         * <p>A table no cell of which conflicts is built as {@link #build()} builds it, whatever is decided for it. A
         * survey merges a table whose merged cells can be placed, and notes it in {@link #merged}.
         *
         * @param id      the identifier of the conflict of the table
         * @param byCells how the conflicting cells are resolved, or {@code null} for a survey
         * @return why the table cannot be built, or {@code null} when it is
         * @throws CellResolutionException when the resolution names what the merged table cannot take
         */
        @Nullable TableConflictReason buildByCells(String id, @Nullable CellResolution byCells) {
            collecting = true;
            notingChanges = byCells == null;
            buildLines();
            if (repeatsProperty()) {
                return TableConflictReason.ROWS;
            }
            renumberColumns();
            if (conflictCells.isEmpty()) {
                return buildRegions() ? null : TableConflictReason.MERGED_CELLS;
            }
            if (byCells == null) {
                if (buildRegions()) {
                    merged = new TableCells(target,
                            cells,
                            regions,
                            conflictCells.stream().sorted(Comparator.comparing(CellConflict::cell)).toList(),
                            changedCells.stream().sorted().toList());
                }
                return TableConflictReason.CELLS;
            }
            decide(id, byCells);
            if (!buildRegions()) {
                return TableConflictReason.MERGED_CELLS;
            }
            requireUncovered(id, byCells);
            return null;
        }

        /**
         * Takes each conflicting cell from the revision chosen for it, and writes the values typed. A conflicting cell
         * left without either is noted as unresolved, and the merge goes on with OUR version of it, so that every
         * conflict left is found at once.
         */
        private void decide(String id, CellResolution byCells) {
            requireInside(id, byCells);
            var unresolved = false;
            for (CellConflict conflict : conflictCells) {
                var cell = tableCellOf(conflict.cell());
                var side = byCells.sideOf(cell);
                if (side == Resolution.THEIRS) {
                    cells.put(conflict.cell(), conflict.theirSource());
                }
                unresolved |= side == null && !byCells.values().containsKey(cell);
            }
            byCells.values().forEach((cell, value) -> {
                conflicts.typed(new Conflicts.TypedValue(id, cell, our.sheet.getSheetName(), value));
                var address = new CellAddress(target.getFirstRow() + cell.row(),
                        target.getFirstColumn() + cell.column());
                cells.put(address, cells.get(address).withTyped(value));
            });
            if (unresolved) {
                conflicts.unresolved(id);
            }
        }

        /**
         * Refuses a resolution that names a cell outside the merged table.
         */
        private void requireInside(String id, CellResolution byCells) {
            var rows = height(target);
            var columns = width(target);
            Stream.concat(byCells.cells().keySet().stream(), byCells.values().keySet().stream())
                    .filter(cell -> cell.row() < 0 || cell.row() >= rows || cell.column() < 0
                            || cell.column() >= columns)
                    .findFirst()
                    .ifPresent(cell -> {
                        throw new CellResolutionException(id, CellResolutionException.Kind.OUTSIDE, cell.row(),
                                cell.column());
                    });
        }

        /**
         * Refuses a value typed for a cell that merged cells cover: only their first cell shows a value.
         */
        private void requireUncovered(String id, CellResolution byCells) {
            for (TableCell cell : byCells.values().keySet()) {
                var row = target.getFirstRow() + cell.row();
                var column = target.getFirstColumn() + cell.column();
                var covered = regions.stream()
                        .anyMatch(region -> region.isInRange(row, column)
                                && (region.getFirstRow() != row || region.getFirstColumn() != column));
                if (covered) {
                    throw new CellResolutionException(id, CellResolutionException.Kind.COVERED, cell.row(),
                            cell.column());
                }
            }
        }

        /**
         * A cell of the merged table by its row and its column in the table.
         */
        private TableCell tableCellOf(CellAddress address) {
            return new TableCell(address.getRow() - target.getFirstRow(),
                    address.getColumn() - target.getFirstColumn());
        }

        /**
         * Builds the cells of every line of the table.
         *
         * @return {@code false} if a cell conflicts and the conflicts are not collected
         */
        private boolean buildLines() {
            for (Side side : Side.values()) {
                var view = views.of(side);
                if (view != null) {
                    var lineOfSide = new int[view.lines()];
                    Arrays.fill(lineOfSide, -1);
                    lineOf.put(side, lineOfSide);
                }
            }
            for (var i = 0; i < lines.lines().size(); i++) {
                var line = lines.lines().get(i);
                for (Side side : Side.values()) {
                    if (line.of(side) >= 0 && lineOf.containsKey(side)) {
                        lineOf.get(side)[line.of(side)] = i;
                    }
                }
                if (!buildLine(i, line)) {
                    return false;
                }
            }
            return true;
        }

        private boolean buildLine(int index, Line line) {
            var positions = views.any().positions();
            for (var position = 0; position < positions; position++) {
                var source = line.merged()
                        ? mergedSourceOf(index, line, position)
                        : takenSourceOf(index, line, position);
                if (source == null) {
                    return false;
                }
                cells.put(address(index, position), source);
            }
            return true;
        }

        /**
         * Where a cell of a line every revision has takes its content from, merged from the revisions. A cell both
         * revisions changed differently is collected with OUR version of what conflicts.
         *
         * @return the content, or {@code null} when the cell conflicts and the conflicts are not collected
         */
        private @Nullable CellSource mergedSourceOf(int index, Line line, int position) {
            var parts = mergeCell(line, index, position);
            var ourAddress = views.our().address(line.our(), position);
            var theirAddress = views.their().address(line.their(), position);
            var source = new CellSource(ourAddress, parts.theirs().isEmpty() ? null : theirAddress, parts.theirs());
            if (parts.conflicting().isEmpty()) {
                return source;
            } else if (!collecting) {
                return null;
            }
            conflictCells.add(new CellConflict(address(index, position),
                    ourAddress,
                    views.base().address(line.base(), position),
                    theirAddress,
                    source,
                    new CellSource(ourAddress, theirAddress, parts.chosen())));
            return source;
        }

        /**
         * Where a cell of a line the merge takes from one revision takes its content from: the revision that inserted
         * the line or changed it alone, or else BASE revision.
         */
        private CellSource takenSourceOf(int index, Line line, int position) {
            if (line.our() >= 0) {
                noteChanged(index, position);
                return new CellSource(views.our().address(line.our(), position), null, Set.of());
            } else if (line.their() >= 0) {
                noteChanged(index, position);
                return new CellSource(null, views.their().address(line.their(), position), ALL_PARTS);
            }
            return CellSource.ofBase(views.base().address(line.base(), position));
        }

        /**
         * Merges the cells of a line every revision has, at a position, and notes the cell as changed when one revision
         * alone changed its value or its comment.
         */
        private XlsSheetMerger.CellParts mergeCell(Line line, int index, int position) {
            var baseView = views.base();
            var ourView = views.our();
            var theirView = views.their();
            baseView.cursor().cell = baseView.cell(line.base(), position);
            ourView.cursor().cell = ourView.cell(line.our(), position);
            theirView.cursor().cell = theirView.cell(line.their(), position);
            var parts = XlsSheetMerger.partsOf(baseView.cursor(), ourView.cursor(), theirView.cursor());
            if (notingChanges && (parts.theirs().contains(CellPart.VALUE)
                    || parts.theirs().contains(CellPart.COMMENT)
                    || XlsSheetMerger.changedInOursAlone(baseView.cursor(), ourView.cursor(), theirView.cursor()))) {
                changedCells.add(address(index, position));
            }
            baseView.cursor().cell = ourView.cursor().cell = theirView.cursor().cell = null;
            return parts;
        }

        /**
         * Notes a cell of a line only one revision has as changed: the revision inserted the line, or changed it while
         * the other one kept it.
         */
        private void noteChanged(int index, int position) {
            if (notingChanges) {
                changedCells.add(address(index, position));
            }
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
         * <p>Where both revisions inserted lines at one place, merged cells a revision stretched over all the lines it
         * inserted there are stretched over the lines both revisions inserted there. Merged cells stretched over some
         * of them only cannot be.
         *
         * @return {@code false} if both revisions changed the merged cells differently, merged cells do not stay over
         * lines next to each other, or they cover a value another revision wrote
         */
        private boolean buildRegions() {
            var keys = new EnumMap<Side, Map<Span, RegionKey>>(Side.class);
            for (Side side : Side.values()) {
                var view = views.of(side);
                if (view != null && !sortRegions(side, view, keys)) {
                    return false;
                }
            }
            if (lines.alignments() == null) {
                // A table taken whole from one revision has every merged cell of it placed with its lines.
                return joinProperties();
            }
            var choice = XlsSheetMerger.choose(Set.copyOf(keys.get(Side.BASE).values()),
                    Set.copyOf(keys.get(Side.OUR).values()),
                    Set.copyOf(keys.get(Side.THEIR).values()),
                    Object::equals);
            if (choice == Choice.CONFLICT) {
                return false;
            }
            var owner = choice == Choice.THEIR ? Side.THEIR : Side.OUR;
            for (var entry : keys.get(owner).entrySet()) {
                if (!place(entry.getKey(), owner, entry.getValue().joints())) {
                    return false;
                }
            }
            return joinProperties();
        }

        /**
         * Joins the properties of the merged table under one properties cell that covers all of them, whichever
         * revision added each property and wherever it added it: above the properties the table has, after them, or
         * as the first ones.
         *
         * @return {@code false} if the properties cell would cover other merged cells
         */
        private boolean joinProperties() {
            var count = propertyLines();
            if (count == 0) {
                return true;
            }
            var first = address(1, 0);
            IntStream.rangeClosed(1, count)
                    .mapToObj(line -> cells.get(address(line, 0)))
                    .filter(this::namesProperties)
                    .findFirst()
                    .ifPresent(label -> cells.put(first, label));
            for (var line = 2; line <= count; line++) {
                cells.put(address(line, 0), CLEARED);
            }
            if (count == 1) {
                return true;
            }
            var region = area(first, address(count, 0));
            if (regions.stream().anyMatch(region::intersects)) {
                return false;
            }
            regions.add(region);
            return true;
        }

        /**
         * The number of properties of the merged table: the rows right under its header that are properties in the
         * revision each of them comes from.
         */
        private int propertyLines() {
            var count = 0;
            while (count + 1 < lines.lines().size() && isProperty(lines.lines().get(count + 1))) {
                count++;
            }
            return count;
        }

        private boolean isProperty(Line line) {
            return Arrays.stream(Side.values())
                    .anyMatch(side -> line.of(side) >= 0 && views.of(side) != null
                            && views.of(side).showsProperties(line.of(side)));
        }

        /**
         * Gives each column THEIR revision inserted into a decision table a name of its own, where OUR revision
         * inserted a column of that name at the same place: both revisions name the condition they add after the
         * last one, such as {@code C3}, and a decision table cannot define a condition twice. The column of THEIR
         * revision takes the next number of its kind, {@code C4}. The names identify the columns inside the table
         * only.
         */
        private void renumberColumns() {
            if (!transposed || !isDecisionTable()) {
                return;
            }
            var position = namesPosition();
            var names = new ArrayList<@Nullable String>();
            for (var line = 0; line < lines.lines().size(); line++) {
                names.add(columnName(line, position));
            }
            var used = new HashSet<String>();
            for (var i = 0; i < names.size(); i++) {
                var name = names.get(i);
                var line = lines.lines().get(i);
                if (name != null && !used.add(name) && line.our() < 0 && line.base() < 0 && lines.jointOf(i) != null) {
                    var renamed = nextName(name, names);
                    names.set(i, renamed);
                    used.add(renamed);
                    var address = address(i, position);
                    cells.put(address, cells.get(address).withTyped(renamed));
                }
            }
        }

        /**
         * Tells whether the merged table is a decision table that names its columns, by the keyword of its header.
         */
        private boolean isDecisionTable() {
            var header = views.any().cell(0, 0);
            if (header == null || header.getCellType() != CellType.STRING) {
                return false;
            }
            var keyword = header.getStringCellValue().strip().split("\\s+", 2)[0];
            return "Rules".equalsIgnoreCase(keyword) || "DT".equalsIgnoreCase(keyword);
        }

        /**
         * The row of a decision table that names its columns: the first one under its header and its properties.
         */
        private int namesPosition() {
            var any = views.any();
            var rows = new View(any.cursor(), any.table(), false);
            var position = 1;
            while (position < rows.lines() && rows.showsProperties(position)) {
                position++;
            }
            return position;
        }

        /**
         * The name a column of the merged decision table takes, such as {@code C1}, or {@code null} when it has none.
         */
        private @Nullable String columnName(int line, int position) {
            var name = valueText(cells.get(address(line, position)));
            return name != null && COLUMN_NAME.matcher(name).matches() ? name : null;
        }

        /**
         * The name of the kind of a column with the number after the highest one the table gives that kind.
         */
        private static String nextName(String name, List<@Nullable String> names) {
            var kind = COLUMN_NAME.matcher(name);
            var prefix = kind.matches() ? kind.group(1) : name;
            var highest = names.stream()
                    .filter(Objects::nonNull)
                    .map(COLUMN_NAME::matcher)
                    .filter(matcher -> matcher.matches() && matcher.group(1).equalsIgnoreCase(prefix))
                    .mapToInt(matcher -> Integer.parseInt(matcher.group(2)))
                    .max()
                    .orElse(0);
            return prefix + (highest + 1);
        }

        /**
         * Tells whether the merged table sets a property twice: both revisions set it, each to another value.
         */
        private boolean repeatsProperty() {
            var names = new HashSet<Long>();
            for (var line = 1; line <= propertyLines(); line++) {
                var name = propertyName(line);
                if (name != 0 && !names.add(name)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * The digest of the name a property of the merged table takes, or {@code 0} when it has none.
         */
        private long propertyName(int line) {
            var cell = valueCell(cells.get(address(line, 1)));
            return cell == null ? 0 : XlsSheetShifts.digest(cell);
        }

        /**
         * Tells whether a cell of the merged table takes the name of the properties as its value.
         */
        private boolean namesProperties(CellSource source) {
            return XlsTableMerger.namesProperties(valueCell(source));
        }

        /**
         * The text a cell of the merged table takes: the value typed for it, or the text of the cell it takes its value
         * from; {@code null} for a cell without text.
         */
        private @Nullable String valueText(@Nullable CellSource source) {
            if (source != null && source.typed() != null) {
                return source.typed().strip();
            }
            var cell = valueCell(source);
            return cell != null && cell.getCellType() == CellType.STRING ? cell.getStringCellValue().strip() : null;
        }

        /**
         * The cell of a revision whose value a cell of the merged table takes, or {@code null} when it takes none.
         */
        private @Nullable Cell valueCell(@Nullable CellSource source) {
            if (source == null) {
                return null;
            }
            View view;
            CellAddress address;
            if (source.base() != null) {
                view = views.base();
                address = source.base();
            } else if (source.our() == null || source.parts().contains(CellPart.VALUE)) {
                view = views.their();
                address = source.their();
            } else {
                view = views.our();
                address = source.our();
            }
            return view == null || address == null
                    ? null
                    : SheetUtil.getCell(view.cursor().sheet, address.getRow(), address.getColumn());
        }

        /**
         * Sorts the merged cells of a revision. Those over lines of BASE revision are kept with their keys, to be
         * merged as a whole. The others are placed at once.
         *
         * @return {@code false} if merged cells cannot be placed
         */
        private boolean sortRegions(Side side, View view, Map<Side, Map<Span, RegionKey>> keys) {
            var sideKeys = new LinkedHashMap<Span, RegionKey>();
            keys.put(side, sideKeys);
            for (Span span : view.regions()) {
                if (isPropertiesCell(view, span)) {
                    // The properties cell is laid over the properties of the merged table once the lines are placed.
                    continue;
                }
                var key = key(side, span);
                if (key == null) {
                    if (!place(span, side, Set.of())) {
                        return false;
                    }
                } else if (!coverWholeJoints(side, span, key)) {
                    return false;
                } else {
                    sideKeys.put(span, key);
                }
            }
            return true;
        }

        /**
         * Tells whether merged cells of a revision are the properties cell of the table: the first cell of its
         * properties, stretched over them.
         */
        private static boolean isPropertiesCell(View view, Span span) {
            return span.firstPosition() == 0 && span.lastPosition() == 0 && view.showsProperties(span.firstLine());
        }

        /**
         * The lines of BASE revision merged cells of a revision cover, the number of lines they cover in that
         * revision, and the places both revisions inserted lines at they stretch over. The lines inserted at those
         * places are not counted, so merged cells both revisions stretched over the lines they inserted there have
         * one key.
         *
         * @return the key, or {@code null} for merged cells over lines the revision inserted only, or of a table
         * taken whole from one revision
         */
        private @Nullable RegionKey key(Side side, Span span) {
            var alignments = lines.alignments();
            if (alignments == null) {
                return null;
            } else if (side == Side.BASE) {
                return new RegionKey(span.firstLine(), span.lastLine(), span.firstPosition(), span.lastPosition(),
                        span.lastLine() - span.firstLine() + 1, Set.of());
            }
            var baseOf = alignments.of(side).baseOf();
            var first = -1;
            var last = -1;
            var length = 0;
            var joints = new HashSet<Joint>();
            for (var line = span.firstLine(); line <= span.lastLine(); line++) {
                var joint = baseOf[line] < 0 ? lines.jointOf(lineOf.get(side)[line]) : null;
                if (joint != null) {
                    joints.add(joint);
                } else {
                    length++;
                }
                if (baseOf[line] >= 0) {
                    first = first < 0 ? baseOf[line] : first;
                    last = baseOf[line];
                }
            }
            return first < 0
                    ? null
                    : new RegionKey(first, last, span.firstPosition(), span.lastPosition(), length, joints);
        }

        /**
         * Tells whether merged cells of a revision cover every line the revision inserted at each place both
         * revisions inserted lines at that the merged cells reach.
         */
        private boolean coverWholeJoints(Side side, Span span, RegionKey key) {
            var lineOfSide = lineOf.get(side);
            return key.joints().stream().allMatch(joint -> {
                var covered = IntStream.rangeClosed(span.firstLine(), span.lastLine())
                        .filter(line -> joint.equals(lines.jointOf(lineOfSide[line])))
                        .count();
                var inserted = IntStream.range(joint.from(), joint.to())
                        .filter(line -> lines.lines().get(line).of(side) >= 0)
                        .count();
                return covered == inserted;
            });
        }

        /**
         * Places merged cells of a revision in the merged table by the lines of that revision, stretched over the
         * lines both revisions inserted at the places given.
         *
         * @param joints the places both revisions inserted lines at that the merged cells stretch over
         * @return {@code false} if those lines are not next to each other in the merged table, or the merged cells
         * cover a value another revision wrote or other merged cells
         */
        private boolean place(Span span, Side owner, Set<Joint> joints) {
            var lineOfSide = lineOf.get(owner);
            var mergedLines = new BitSet();
            for (var line = span.firstLine(); line <= span.lastLine(); line++) {
                if (lineOfSide[line] < 0) {
                    return false;
                }
                mergedLines.set(lineOfSide[line]);
            }
            joints.forEach(joint -> mergedLines.set(joint.from(), joint.to()));
            var first = mergedLines.nextSetBit(0);
            var last = mergedLines.length() - 1;
            if (mergedLines.cardinality() != last - first + 1) {
                return false;
            }
            for (var line = first; line <= last; line++) {
                for (var position = span.firstPosition(); position <= span.lastPosition(); position++) {
                    var covered = line != first || position != span.firstPosition();
                    if (covered && hidesValue(line, position, owner)) {
                        return false;
                    }
                }
            }
            var region = area(address(first, span.firstPosition()), address(last, span.lastPosition()));
            if (regions.contains(region)) {
                // Both revisions inserted the line alike with these merged cells.
                return true;
            } else if (regions.stream().anyMatch(region::intersects)) {
                return false;
            }
            regions.add(region);
            return true;
        }

        /**
         * Tells whether a cell of the merged table under merged cells of one revision takes a value the other revision
         * shows. A value the other revision keeps under merged cells of its own is hidden there as well.
         */
        private boolean hidesValue(int line, int position, Side owner) {
            var source = cells.get(address(line, position));
            if (source.base() != null || source.typed() != null) {
                // Only a table taken whole from BASE revision takes its cells from it, merged cells included. A value
                // typed under merged cells is refused on its own.
                return false;
            }
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
