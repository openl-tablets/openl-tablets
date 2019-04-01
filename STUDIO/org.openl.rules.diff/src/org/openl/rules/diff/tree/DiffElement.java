package org.openl.rules.diff.tree;

import org.openl.rules.diff.hierarchy.Projection;

/**
 * Element in a Diff Tree that corresponds to particular comparing data.
 *
 * @author Aleh Bykhavets
 *
 */
public interface DiffElement {
    /**
     * Difference status of the element compared to original one.
     * <p>
     * Exception: for an original element DiffStatus has other meaning.
     * <ul>
     * <li>{@link DiffStatus#ORIGINAL} shows that it is present in original data</li>
     * <li>{@link DiffStatus#ORIGINAL_ABSENT} shows that it is absent in original data but exists in at least one
     * comparing data</li>
     * </ul>
     * Other values are considered non valid.
     *
     * @return difference status
     */
    DiffStatus getDiffStatus();

    /**
     * Shows whether hierarchy of all children, regardless of their properties, are the same for compared and original
     * elements.
     *
     * @return {@code true} if hierarchy of children are the same
     */
    boolean isHierarhyEqual();

    /**
     * Shows whether hierarchy and properties of all children are the same for compared and original elements.
     *
     * @return {@code true} if hierarchy and properties of children are completely the same
     */
    boolean isChildrenEqual();

    /**
     * Shows whether compared and original elements have the same properties. Ignores children elements.
     *
     * @return {@code true} if properties are the same
     */
    boolean isSelfEqual();

    /**
     * Projection of the element.
     * <p>
     * In case if some comparing data has no such element it will return {@literal null}.
     *
     * @return reference on projection of element or {@literal null}
     */
    Projection getProjection();
}
