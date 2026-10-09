package org.openl.rules.xls.merge.diff;

/**
 * Conflict Statuses
 *
 * @author Vladyslav Pikus
 */
public enum DiffStatus {

    /**
     * Sheet has changes in OUR and THEIR revisions comparing to BASE revision, which cannot be merged
     */
    CONFLICT,
    /**
     * Sheet has changes in OUR and THEIR revisions comparing to BASE revision that do not overlap: in different
     * cells, or in different parts of a cell, such as its value in one revision and its style in the other. The
     * changes of THEIR revision are applied cell by cell
     */
    MERGED,
    /**
     * Sheet has changes in OUR revision only comparing to BASE revision
     */
    OUR,
    /**
     * Sheet has changes in THEIR revision only comparing to BASE revision
     */
    THEIR

}
