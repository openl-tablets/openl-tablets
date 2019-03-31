package org.openl.rules.diff.tree;

/**
 * Result of comparing 2 elements.
 * <p>
 * As a rule the 2nd element is treated as it is some version of 1st one. But that is not always true.
 * 
 * @author Aleh Bykhavets
 * 
 */
public enum DiffStatus {
    /**
     * An element was added. There is no such element in original data.
     */
    ADDED,

    /**
     * An element was removed. Some element exists in original data but absents in comparing data.
     */
    REMOVED,

    /**
     * An element is equal to original.
     */
    EQUALS,

    /**
     * An element is different. Original and comparing data have difference.
     */
    DIFFERS,

    /**
     * Original version
     */
    ORIGINAL,

    /**
     * Original version -- no corresponding element
     */
    ORIGINAL_ABSENT
}
