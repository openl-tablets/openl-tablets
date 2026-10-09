package org.openl.rules.xls.merge.diff;

import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.util.CellAddress;

/**
 * What a sheet changed in both revisions takes from THEIR revision, when the changes are merged cell by cell.
 *
 * <p>Everything not listed stays as OUR revision has it: OUR revision either changed it or left it as in BASE
 * revision.
 *
 * @param cells          cells that take parts from THEIR revision, with the parts they take; a part THEIR revision
 *                       has not got is cleared
 * @param rows           rows whose height and visibility are taken from THEIR revision
 * @param columns        columns whose width and visibility are taken from THEIR revision
 * @param defaultLayout  {@code true} when the default column width and row height of the sheet are taken from THEIR
 *                       revision
 * @param mergedRegions  {@code true} when the merged cells of the sheet are taken from THEIR revision
 * @param pictures       {@code true} when the pictures of the sheet are taken from THEIR revision
 * @param visibility     {@code true} when the visibility of the sheet is taken from THEIR revision
 */
public record SheetMergePlan(Map<CellAddress, Set<CellPart>> cells,
                             Set<Integer> rows,
                             Set<Integer> columns,
                             boolean defaultLayout,
                             boolean mergedRegions,
                             boolean pictures,
                             boolean visibility) {
}
