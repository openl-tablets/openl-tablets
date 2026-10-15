package org.openl.rules.xls.merge.diff;

/**
 * How a conflict of a workbook is resolved: the merged workbook takes what conflicts, a sheet or a table, whole from
 * one revision.
 */
public enum Resolution {

    /**
     * Take it from OUR revision, the one of the branch merged into
     */
    OURS,
    /**
     * Take it from THEIR revision, the one of the branch merged in
     */
    THEIRS,
    /**
     * Take it from BASE revision, the one both branches started from, without the changes of either branch
     */
    BASE

}
