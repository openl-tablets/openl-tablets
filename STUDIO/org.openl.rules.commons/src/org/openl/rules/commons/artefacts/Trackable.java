package org.openl.rules.commons.artefacts;

/**
 * Trackable Interface.
 * <p/>
 * It is used to track changes in an object.
 */
public interface Trackable {
    /**
     * Checks whether the object was created recently.
     *
     * @return <code>true</code> if it is new;
     *         <code>false</code> otherwise.
     */
    boolean isNew();

    /**
     * Checks whether the object was changed.
     *
     * @return <code>true</coed> if it was changed;
     *         <code>false</code> if remains the same.
     */
    boolean isChanged();

//    boolean isDeleted();
}
