package org.openl.rules.repository.common;

/**
 * For retrieving a current revision of a repository.
 *
 * @author Yury Molchan
 */
public interface RevisionGetter {
    /**
     * Should return an object that persists a current change set revision. This object must implement
     * {@link Object#equals(Object)} method which must return true if no changes were detected between two change sets.
     */
    Object getRevision();
}
