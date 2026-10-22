package org.openl.rules.xls.merge.diff;

import org.apache.poi.ss.util.CellRangeAddress;
import org.jspecify.annotations.Nullable;

/**
 * A table both revisions changed in ways the merge cannot combine. It is resolved by taking it whole from one revision,
 * or, when only some of its cells conflict, cell by cell.
 *
 * <p>A table that a revision removed has no place in that revision. A table both revisions added has no place in BASE
 * revision.
 *
 * @param id     identifies the conflict within its workbook, for a resolution to name it
 * @param ours   the place of the table in OUR revision, or {@code null} when OUR revision has no such table
 * @param base   the place of the table in BASE revision, or {@code null} when both revisions added it
 * @param theirs the place of the table in THEIR revision, or {@code null} when THEIR revision has no such table
 * @param reason why the changes of the table cannot be combined
 * @param cells  the table merged cell by cell, or {@code null} when it is resolved only as a whole: its cells conflict
 *               and nothing else, and a survey found them
 */
public record TableConflict(String id,
                            @Nullable CellRangeAddress ours,
                            @Nullable CellRangeAddress base,
                            @Nullable CellRangeAddress theirs,
                            TableConflictReason reason,
                            @Nullable TableCells cells) {

    public TableConflict(String id,
                         @Nullable CellRangeAddress ours,
                         @Nullable CellRangeAddress base,
                         @Nullable CellRangeAddress theirs,
                         TableConflictReason reason) {
        this(id, ours, base, theirs, reason, null);
    }
}
