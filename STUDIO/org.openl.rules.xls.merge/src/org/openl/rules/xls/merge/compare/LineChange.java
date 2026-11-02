package org.openl.rules.xls.merge.compare;

/**
 * A row or a column of a compared table, as the two versions of the table hold it.
 *
 * <p>A line both versions hold is {@link ChangeStatus#EQUAL} or {@link ChangeStatus#CHANGED}, when a cell of it
 * differs. A line one version holds alone is {@link ChangeStatus#ADDED} or {@link ChangeStatus#REMOVED}.
 *
 * @param first  the line in the first version, counted from the first line of the table from 0, or {@value #NONE}
 *               when the first version does not hold it
 * @param second the line in the second version, counted the same way, or {@value #NONE} when the second version does
 *               not hold it
 * @param status what became of the line
 */
public record LineChange(int first, int second, ChangeStatus status) {

    /**
     * The line of a version that does not hold the line.
     */
    public static final int NONE = -1;
}
