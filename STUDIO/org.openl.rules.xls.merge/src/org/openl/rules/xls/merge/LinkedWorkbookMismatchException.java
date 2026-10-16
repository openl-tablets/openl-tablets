package org.openl.rules.xls.merge;

import lombok.Getter;

/**
 * Tells that a workbook cannot be merged: a formula the merge copies into it reads another workbook through a link
 * the workbook numbers differently, so the formula would read another workbook there.
 */
@Getter
public final class LinkedWorkbookMismatchException extends IllegalStateException {

    /** The sheet the formula is copied into. */
    private final String sheet;

    /** The cell the formula is copied into, in A1 notation. */
    private final String cell;

    /** The number of the link the formula names. */
    private final int link;

    LinkedWorkbookMismatchException(String sheet, String cell, int link) {
        super(("The formula copied into %s!%s reads the workbook linked under the number %d, which the merged "
                + "workbook links to another workbook or none.").formatted(sheet, cell, link));
        this.sheet = sheet;
        this.cell = cell;
        this.link = link;
    }
}
