package org.openl.rules.xls.merge.compare;

/**
 * A row or a column of a table, as the base version and the two versions of a conflict hold it.
 *
 * <p>A line of the base stands against the line each version made of it: equal, changed, or removed when the version
 * does not hold it. A line a version inserted stands against no line of the base and none of the other version: it is
 * {@link ChangeStatus#ADDED} for that version and {@link ChangeStatus#EQUAL} for the other one, which did nothing
 * there.
 *
 * @param base         the line in the base, counted from the first line of the table from 0, or
 *                     {@value LineChange#NONE} for a line a version inserted
 * @param yours        the line in your version, or {@value LineChange#NONE} when it does not hold the line
 * @param theirs       the line in their version, or {@value LineChange#NONE} when it does not hold the line
 * @param yoursStatus  what your version did to the line
 * @param theirsStatus what their version did to the line
 */
public record ConflictLine(int base, int yours, int theirs, ChangeStatus yoursStatus, ChangeStatus theirsStatus) {
}
