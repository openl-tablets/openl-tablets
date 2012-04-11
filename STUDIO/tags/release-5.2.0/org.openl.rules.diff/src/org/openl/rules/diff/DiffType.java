package org.openl.rules.diff;

/**
 * Difference type.
 *
 * @author Andrey Naumenko
 */
public enum DiffType {
    Addition,
    Deletion,
    Equal,
    EqualWithDifferentChildren;
}
