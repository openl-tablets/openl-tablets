package org.openl.rules.xls.merge.diff;

/**
 * Conflict Statuses
 *
 * @author Vladyslav Pikus
 */
public enum DiffStatus {

    /**
     * Sheet has changes in OUR and THEIR revisions comparing to BASE revision
     */
    CONFLICT,
    /**
     * Sheet has changes in OUR revision only comparing to BASE revision
     */
    OUR,
    /**
     * Sheet has changes in THEIR revision only comparing to BASE revision
     */
    THEIR

}