package org.openl.rules.commons.artefacts;

/**
 * Visitor for Linked Entities.
 */
public interface LinkedEntityVisitor {
    /**
     * An Artefact invokes this method in {@link Artefact#accept(LinkedEntityVisitor)} method.
     * <p/>
     * If visitor is interested in traversing an artefact tree deeper it should return <code>true</code>.
     *
     * @param linkedEntity linked entity to be visited
     * @return <code>true</code> if visitor is going to visit children of the artefact;
     *         <code>false</code> otherwise.
     */
    boolean visit(LinkedEntity linkedEntity);

    /**
     * This method is called after visiting children in an artefact tree.
     * <p/>
     * If {@link #visit(LinkedEntity)} returns <code>false</code> this method will not be invoked.
     * <p/>
     * It is a place for post actions.
     *
     * @param linkedEntity
     */
    void afterVisit(LinkedEntity linkedEntity);
}
