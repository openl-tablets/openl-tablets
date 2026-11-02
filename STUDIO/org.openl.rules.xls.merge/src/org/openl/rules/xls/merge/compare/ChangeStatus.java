package org.openl.rules.xls.merge.compare;

/**
 * What became of an element of the first version of a workbook in the second one: a sheet, a table, a row or a column.
 */
public enum ChangeStatus {

    /**
     * Both versions hold the element alike.
     */
    EQUAL,
    /**
     * Both versions hold the element, and it differs.
     */
    CHANGED,
    /**
     * Both versions hold the table alike, and the second one holds it at another place it was moved to on purpose. A
     * table only pushed by the rows inserted or deleted above it is not moved.
     */
    MOVED,
    /**
     * Only the second version holds the element.
     */
    ADDED,
    /**
     * Only the first version holds the element.
     */
    REMOVED
}
