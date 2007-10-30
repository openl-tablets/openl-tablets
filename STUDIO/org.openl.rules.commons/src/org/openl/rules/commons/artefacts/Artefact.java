package org.openl.rules.commons.artefacts;

import java.util.Collection;

/**
 * Artefact.
 * Relations between artefacts form tree of artefacts.
 * Tree of artefacts define hierarchical structure of presentation data.
 * <p/>
 * Artefact is Composite by default.
 * You can create implementation for Leaf where throw unsupported exception
 * in child related methods.
 */
public interface Artefact {

    /**
     * Gets name of the artefact.
     *
     * @return name of artefact
     */
    String getName();

    /**
     * Gets parent artefact for this one.
     *
     * @return parent artefact
     */
    Artefact getParent();

    /**
     * Gets a project that contains the artefact.
     *
     * @return project in which the artefact is
     */
    Project getProject();

    /**
     * Gets path of the artefact.
     *
     * @return artefact path
     * @throws ArtefactException
     */
    ArtefactPath getArtefactPath() throws ArtefactException;

    // --- Composite

    /**
     * Visits the artefact and all its children recursively.
     *
     * @param visitor artefact visitor
     * @throws ArtefactException
     */
    void accept(ArtefactVisitor visitor) throws ArtefactException;

    /**
     * Checks whether the Artefact is a leaf.
     * <p/>
     * If the Artefact is composite but has no children it can pretend to be a leaf,
     * even if it is temporarily.
     *
     * @return <code>true</code> if this artefact is a leaf.
     */
    boolean isLeaf();

    /**
     * Deletes the artefact and all its children (if any).
     *
     * @throws ArtefactException
     */
    void delete() throws ArtefactException;

    /**
     * Checks whether the artefact has child with given name.
     *
     * @param name name of child artefact to be checked
     * @return <code>true</code> if child with given name exists
     */
    boolean hasArtefact(String name);

    /**
     * Returns artefact with specified name.
     *
     * @param name name of artefact to be returned
     * @return reference on child artefact
     * @throws ArtefactException if there is no artefact with such name
     */
    Artefact getArtefact(String name) throws ArtefactException;

    /**
     * Gets collection of child artefacts.
     *
     * @return child artefacts
     * @throws ArtefactException
     */
    Collection<Artefact> getArtefacts() throws ArtefactException;

    /**
     * Adds artefact as a child.
     *
     * @param artefact artefact to be added.
     * @throws ArtefactException
     */
    void addArtefact(Artefact artefact) throws ArtefactException;

    /**
     * Removes child artefact from composition.
     *
     * @param name name of artefact
     * @return removed artefact
     * @throws ArtefactException if there is no artefact with such name
     */
    Artefact removeArtefact(String name) throws ArtefactException;

    // --- Linked Entity

    /**
     * Returns a entity that is linked with the artefact.
     *
     * @return reference on linked entity
     */
    LinkedEntity getLinkedEntity();

    /**
     * Visits linked entities of the artefact and all its children.
     *
     * @param visitor linked entity visitor
     * @throws ArtefactException
     */
    void accept(LinkedEntityVisitor visitor) throws ArtefactException;

    /**
     * Sets reference on linked entity.
     * I.e. links entity with the artefact.
     *
     * @param linkedEntity entity to be linked with
     */
    void setLinkedEntity(LinkedEntity linkedEntity);
}
