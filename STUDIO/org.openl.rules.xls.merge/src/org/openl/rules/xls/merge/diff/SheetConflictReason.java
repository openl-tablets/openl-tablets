package org.openl.rules.xls.merge.diff;

/**
 * Why a sheet both revisions changed is resolved only as a whole.
 */
public enum SheetConflictReason {

    /**
     * One revision removed the sheet, and the other one changed it
     */
    REMOVED_AND_CHANGED,
    /**
     * Both revisions added a sheet of the same name with different content
     */
    ADDED_IN_BOTH,
    /**
     * Both revisions hid or showed the sheet differently
     */
    VISIBILITY,
    /**
     * Both revisions changed the pictures of the sheet differently
     */
    PICTURES,
    /**
     * THEIR revision changed the data validation, the conditional formatting, the hyperlinks or the frozen panes of
     * the sheet, which the merge does not take
     */
    SETTINGS,
    /**
     * Tables of the two revisions would touch each other and be read as one table
     */
    TOUCHING_TABLES,
    /**
     * Content that moves is referred to by a formula, a defined name, a data validation, a conditional formatting or a
     * hyperlink, or lies under a picture, a chart, an Excel table or an autofilter
     */
    MOVED_CONTENT

}
