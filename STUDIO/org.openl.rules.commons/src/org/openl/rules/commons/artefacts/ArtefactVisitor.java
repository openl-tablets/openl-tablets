package org.openl.rules.commons.artefacts;

/**
 * Visitor for Artefacts.
 */
public interface ArtefactVisitor {
    /**
     * An Artefact invokes this method in {@link Artefact#accept(ArtefactVisitor)} method.
     * <p/>
     * If the visitor wants to visit children of the artefact it should return <code>true</code>.
     * If it is not interested in children it should return <code>false</code>.
     *
     * @param artefact artefact to be visited
     * @return <code>true</code> if visitor is going to visit children of the artefact;
     *         <code>false</code> otherwise.
     */
    boolean visit(Artefact artefact);
}
