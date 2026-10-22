package org.openl.rules.xls.merge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import org.openl.rules.xls.merge.diff.CellResolution;
import org.openl.rules.xls.merge.diff.CellResolutionException;
import org.openl.rules.xls.merge.diff.Resolution;
import org.openl.rules.xls.merge.diff.SheetConflictReason;
import org.openl.rules.xls.merge.diff.TableCell;
import org.openl.rules.xls.merge.diff.TableConflict;
import org.openl.rules.xls.merge.diff.UnresolvedConflictsException;

/**
 * What a merge of a workbook does with the places both revisions changed in ways it cannot combine by itself.
 *
 * <p>A strict merge leaves the sheet a conflict, the way a merge of branches does. A survey notes every conflicting
 * table of a sheet and goes on with OUR version of it, so the conflicts after it are found too. A resolution takes
 * every conflict as decided, and notes the conflicts that have no decision.
 *
 * <p>A sheet or the palette of a workbook is decided as a whole. A decision for a sheet stands for every table of the
 * sheet. A table is decided as a whole, or cell by cell when only some of its cells conflict.
 */
@NullMarked
final class Conflicts {

    private enum Mode {
        STRICT,
        SURVEY,
        RESOLUTION
    }

    /**
     * Orders the conflicting tables of a sheet by their places: in BASE revision, or in THEIR revision for tables both
     * revisions added.
     */
    private static final Comparator<TableConflict> TOP_TO_BOTTOM = Comparator
            .comparingInt((TableConflict table) -> placeOf(table).getFirstRow())
            .thenComparingInt(table -> placeOf(table).getFirstColumn());

    private final Mode mode;
    private final Map<String, Resolution> decisions;
    /**
     * The tables a resolution resolves cell by cell, by the identifiers of their conflicts.
     */
    private final Map<String, CellResolution> byCells;
    /**
     * The conflicting tables of the current sheet a survey noted.
     */
    private final List<TableConflict> tables = new ArrayList<>();
    /**
     * Why the current sheet is resolved only as a whole, as its merge last told it.
     */
    private @Nullable SheetConflictReason refusal;
    /**
     * The conflicts a resolution has no decision for.
     */
    private final List<String> unresolved = new ArrayList<>();
    /**
     * The tables a resolution resolves cell by cell that cannot be merged so.
     */
    private final List<String> notByCells = new ArrayList<>();
    /**
     * The sheets a resolution cannot merge as decided.
     */
    private final Map<String, SheetConflictReason> refused = new LinkedHashMap<>();
    /**
     * The values typed for the cells of the tables a resolution resolves cell by cell.
     */
    private final List<TypedValue> typedValues = new ArrayList<>();

    /**
     * A value typed for a cell of a table resolved cell by cell.
     *
     * @param table the identifier of the conflict of the table
     * @param cell  the cell of the merged table
     * @param sheet the name of the sheet of the table
     * @param value the value as typed
     */
    record TypedValue(String table, TableCell cell, String sheet, String value) {
    }

    private Conflicts(Mode mode, Map<String, Resolution> decisions, Map<String, CellResolution> byCells) {
        this.mode = mode;
        this.decisions = Map.copyOf(decisions);
        this.byCells = Map.copyOf(byCells);
    }

    /**
     * Leaves a sheet that both revisions changed in ways the merge cannot combine a conflict.
     */
    static Conflicts strict() {
        return new Conflicts(Mode.STRICT, Map.of(), Map.of());
    }

    /**
     * Notes every conflict, going on with OUR version of what conflicts.
     */
    static Conflicts survey() {
        return new Conflicts(Mode.SURVEY, Map.of(), Map.of());
    }

    /**
     * Resolves every conflict as decided.
     *
     * @param decisions how each conflict is resolved, by the identifier of the conflict
     */
    static Conflicts resolution(Map<String, Resolution> decisions) {
        return resolution(decisions, Map.of());
    }

    /**
     * Resolves every conflict as decided, some tables cell by cell.
     *
     * @param decisions how each conflict is resolved as a whole, by the identifier of the conflict
     * @param byCells   how each table resolved cell by cell is resolved, by the identifier of its conflict; no table
     *                  is decided both ways
     */
    static Conflicts resolution(Map<String, Resolution> decisions, Map<String, CellResolution> byCells) {
        if (byCells.keySet().stream().anyMatch(decisions::containsKey)) {
            throw new IllegalArgumentException("A table is resolved either as a whole or cell by cell.");
        }
        return new Conflicts(Mode.RESOLUTION, decisions, byCells);
    }

    /**
     * Starts the merge of a sheet.
     */
    void startSheet() {
        tables.clear();
        refusal = null;
    }

    /**
     * What a resolution noted so far, by how many conflicts of each kind it noted.
     */
    record Mark(int unresolved, int notByCells, int typedValues) {
    }

    /**
     * Marks what the merge of the current sheet noted so far, for {@link #rewind(Mark)}.
     */
    Mark mark() {
        return new Mark(unresolved.size(), notByCells.size(), typedValues.size());
    }

    /**
     * Forgets what the merge of the current sheet noted since the mark, so that the sheet can be merged again.
     */
    void rewind(Mark mark) {
        startSheet();
        unresolved.subList(mark.unresolved(), unresolved.size()).clear();
        notByCells.subList(mark.notByCells(), notByCells.size()).clear();
        typedValues.subList(mark.typedValues(), typedValues.size()).clear();
    }

    /**
     * The resolution decided for a sheet or the palette as a whole.
     *
     * @return the resolution, or {@code null} when none is decided; only a resolution decides anything
     */
    @Nullable Resolution decided(String id) {
        return mode == Mode.RESOLUTION ? decisions.get(id) : null;
    }

    /**
     * Tells whether the merge notes the conflicts it finds, and merges the cells of a table that can be merged cell by
     * cell, so that its conflicting cells are listed.
     */
    boolean surveys() {
        return mode == Mode.SURVEY;
    }

    /**
     * How a table is resolved cell by cell.
     *
     * @return the resolution, or {@code null} when none is decided; only a resolution decides anything
     */
    @Nullable CellResolution decidedByCells(String id) {
        return mode == Mode.RESOLUTION ? byCells.get(id) : null;
    }

    /**
     * Resolves a table both revisions changed in ways the merge cannot combine.
     *
     * <p>A table a resolution decided cell by cell comes here only when it cannot be merged so.
     *
     * @return how the table is taken, or nothing when the sheet stays a conflict
     */
    Optional<Resolution> resolve(TableConflict conflict) {
        return switch (mode) {
            case STRICT -> Optional.empty();
            case SURVEY -> {
                tables.add(conflict);
                yield Optional.of(Resolution.OURS);
            }
            case RESOLUTION -> {
                var decision = decisions.get(conflict.id());
                if (byCells.containsKey(conflict.id())) {
                    notByCells.add(conflict.id());
                } else if (decision == null) {
                    unresolved.add(conflict.id());
                }
                // The merge goes on without a decision, so that every conflict left is found at once.
                yield Optional.of(decision == null ? Resolution.OURS : decision);
            }
        };
    }

    /**
     * Tells why the current sheet is resolved only as a whole.
     */
    void refuse(SheetConflictReason reason) {
        refusal = reason;
    }

    /**
     * Why the current sheet is resolved only as a whole, as its merge last told it.
     */
    @Nullable SheetConflictReason refusal() {
        return refusal;
    }

    /**
     * The conflicting tables of the current sheet a survey noted, from top to bottom.
     */
    List<TableConflict> tables() {
        return tables.stream().sorted(TOP_TO_BOTTOM).toList();
    }

    private static CellRangeAddress placeOf(TableConflict table) {
        var base = table.base();
        return base != null ? base : Objects.requireNonNull(table.theirs());
    }

    /**
     * Notes a sheet a resolution cannot merge without a decision for the sheet as a whole.
     *
     * @param reason why the sheet is resolved only as a whole, or {@code null} when its merge did not tell it
     */
    void unresolvedSheet(String sheet, @Nullable SheetConflictReason reason) {
        if (mode != Mode.RESOLUTION) {
            return;
        }
        if (reason == null) {
            unresolved.add(sheet);
        } else {
            refused.put(sheet, reason);
        }
    }

    /**
     * Notes a conflict, other than a table, a resolution has no decision for.
     */
    void unresolved(String id) {
        if (mode == Mode.RESOLUTION) {
            unresolved.add(id);
        }
    }

    /**
     * Notes a value typed for a cell of a table resolved cell by cell, so that a formula is read once every sheet of
     * the merged workbook is known.
     */
    void typed(TypedValue value) {
        typedValues.add(value);
    }

    /**
     * Refuses a resolution that types a formula the merged workbook cannot read.
     *
     * @param our      OUR revision of the workbook, which knows the names of the merged workbook
     * @param hasSheet tells whether the merged workbook has a sheet, by its name
     * @throws CellResolutionException when a typed formula cannot be read
     */
    void requireReadable(Workbook our, Predicate<String> hasSheet) {
        typedValues.stream()
                .filter(typed -> TypedValues.isUnreadableFormula(typed.value(), our.getSheet(typed.sheet()), hasSheet))
                .findFirst()
                .ifPresent(typed -> {
                    throw new CellResolutionException(typed.table(), CellResolutionException.Kind.FORMULA,
                            typed.cell().row(), typed.cell().column());
                });
    }

    /**
     * Refuses a resolution that leaves a conflict.
     *
     * @throws UnresolvedConflictsException when a conflict has no decision, a table decided cell by cell cannot be
     *                                      merged so, or a sheet cannot be merged as decided
     */
    void requireResolved() throws UnresolvedConflictsException {
        if (!unresolved.isEmpty() || !notByCells.isEmpty() || !refused.isEmpty()) {
            throw new UnresolvedConflictsException(unresolved, notByCells, refused);
        }
    }
}
