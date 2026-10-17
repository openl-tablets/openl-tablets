package org.openl.rules.xls.merge.diff;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.jspecify.annotations.Nullable;

/**
 * How a sheet changed in both revisions is merged into OUR revision.
 *
 * <p>Everything not listed stays as OUR revision has it: OUR revision either changed it or left it as in BASE
 * revision.
 *
 * @param cells         cells of OUR revision that change, with the content each takes
 * @param rows          rows whose height and visibility are taken from THEIR revision
 * @param columns       columns whose width and visibility are taken from THEIR revision
 * @param defaultLayout {@code true} when the default column width and row height of the sheet are taken from THEIR
 *                      revision
 * @param mergedRegions all the merged cells of the merged sheet, or {@code null} to keep those of OUR revision
 * @param pictures      {@code true} when the pictures of the sheet are taken from THEIR revision
 * @param visibility    {@code true} when the visibility of the sheet is taken from THEIR revision
 * @param panes         {@code true} when the frozen or split panes of the sheet are taken from THEIR revision
 * @param conditionalFormatting {@code true} when the conditional formatting of the sheet is taken from THEIR revision
 */
public record SheetMergePlan(Map<CellAddress, CellSource> cells,
                             Set<Integer> rows,
                             Set<Integer> columns,
                             boolean defaultLayout,
                             @Nullable List<CellRangeAddress> mergedRegions,
                             boolean pictures,
                             boolean visibility,
                             boolean panes,
                             boolean conditionalFormatting) {

    /**
     * Tells whether a cell of OUR revision moves to another address, so the content of OUR revision is read before
     * the sheet changes.
     */
    public boolean movesCells() {
        return movesCells(cells);
    }

    /**
     * Tells whether a cell of OUR revision moves to another address.
     *
     * @param cells the cells of OUR revision that change, with the content they take
     */
    public static boolean movesCells(Map<CellAddress, CellSource> cells) {
        return cells.entrySet()
                .stream()
                .anyMatch(entry -> entry.getValue().our() != null && !entry.getKey().equals(entry.getValue().our()));
    }

    /**
     * Tells whether a cell is taken from BASE revision, so BASE revision is read for the merge.
     */
    boolean takesBase() {
        return cells.values().stream().anyMatch(source -> source.base() != null);
    }
}
