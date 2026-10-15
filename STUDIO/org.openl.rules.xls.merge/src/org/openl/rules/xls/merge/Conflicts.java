package org.openl.rules.xls.merge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.poi.ss.util.CellRangeAddress;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import org.openl.rules.xls.merge.diff.Resolution;
import org.openl.rules.xls.merge.diff.SheetConflictReason;
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
 * sheet.
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
     * The sheets a resolution cannot merge as decided.
     */
    private final Map<String, SheetConflictReason> refused = new LinkedHashMap<>();

    private Conflicts(Mode mode, Map<String, Resolution> decisions) {
        this.mode = mode;
        this.decisions = Map.copyOf(decisions);
    }

    /**
     * Leaves a sheet that both revisions changed in ways the merge cannot combine a conflict.
     */
    static Conflicts strict() {
        return new Conflicts(Mode.STRICT, Map.of());
    }

    /**
     * Notes every conflict, going on with OUR version of what conflicts.
     */
    static Conflicts survey() {
        return new Conflicts(Mode.SURVEY, Map.of());
    }

    /**
     * Resolves every conflict as decided.
     *
     * @param decisions how each conflict is resolved, by the identifier of the conflict
     */
    static Conflicts resolution(Map<String, Resolution> decisions) {
        return new Conflicts(Mode.RESOLUTION, decisions);
    }

    /**
     * Starts the merge of a sheet.
     */
    void startSheet() {
        tables.clear();
        refusal = null;
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
     * Resolves a table both revisions changed in ways the merge cannot combine.
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
                if (decision == null) {
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
     * Refuses a resolution that leaves a conflict.
     *
     * @throws UnresolvedConflictsException when a conflict has no decision, or a sheet cannot be merged as decided
     */
    void requireResolved() throws UnresolvedConflictsException {
        if (!unresolved.isEmpty() || !refused.isEmpty()) {
            throw new UnresolvedConflictsException(unresolved, refused);
        }
    }
}
