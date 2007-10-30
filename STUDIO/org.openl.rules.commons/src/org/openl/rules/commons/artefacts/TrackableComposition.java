package org.openl.rules.commons.artefacts;

/**
 * Trackable Composition Interface.
 * <p/>
 * It is addition to Trackable interface whan an object can have children.
 *
 */
public interface TrackableComposition<T> {
    /**
     * Checks whether at least one child in the composition was changed.
     *
     * @return <code>true</code> if at least one child was changed;
     *         <code>false</code> otherwise.
     */
    boolean isAnyChildChanged();

    /**
     * Notifies parent entities that an child entity was changed.
     * <p/>
     * When an entity is changing it should inform its parent that it was changed.
     * The parent in turn can inform own parent and so on.
     *
     * @param entity changed entity
     */
    void childChanged(T entity);
}
