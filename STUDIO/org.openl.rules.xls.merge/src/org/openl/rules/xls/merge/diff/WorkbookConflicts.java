package org.openl.rules.xls.merge.diff;

import java.util.List;

/**
 * What both revisions of a workbook changed in ways the merge cannot combine, sheet by sheet, and how the merge takes
 * the other sheets either revision changed.
 *
 * @param sheets  the sheets either revision changed, in the order of OUR revision and then of THEIR revision
 * @param palette whether both revisions of an .xls workbook changed the colours of its palette differently; the
 *                palette is resolved as a whole, by OUR or THEIR revision, under the identifier {@link #PALETTE_ID}
 */
public record WorkbookConflicts(List<SheetConflicts> sheets, boolean palette) {

    /**
     * Identifies the palette of an .xls workbook for a resolution. No sheet has this name: a sheet name cannot hold a
     * slash.
     */
    public static final String PALETTE_ID = "/palette";
}
