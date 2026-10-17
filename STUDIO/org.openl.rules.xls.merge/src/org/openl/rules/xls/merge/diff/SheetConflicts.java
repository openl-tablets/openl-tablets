package org.openl.rules.xls.merge.diff;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * A sheet either revision changed, how the merge takes it, and what of it is left to resolve.
 *
 * <p>A sheet of the {@link DiffStatus#CONFLICT} status is resolved either as a whole, when it has a reason, or table by
 * table. Either way the sheet itself can be taken whole from one revision, which resolves its tables too. The name of
 * the sheet identifies the sheet for a resolution.
 *
 * @param name     the name of the sheet
 * @param status   how the merge takes the sheet by itself
 * @param reason   why the sheet is resolved only as a whole, or {@code null} when its tables are resolved one by one
 * @param inOurs   whether OUR revision has the sheet
 * @param inBase   whether BASE revision has the sheet
 * @param inTheirs whether THEIR revision has the sheet
 * @param tables   the tables of the sheet to resolve, from top to bottom; empty unless the sheet is resolved table by
 *                 table
 */
public record SheetConflicts(String name,
                             DiffStatus status,
                             @Nullable SheetConflictReason reason,
                             boolean inOurs,
                             boolean inBase,
                             boolean inTheirs,
                             List<TableConflict> tables) {
}
