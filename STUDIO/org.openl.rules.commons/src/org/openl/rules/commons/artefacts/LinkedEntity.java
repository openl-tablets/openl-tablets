package org.openl.rules.commons.artefacts;

import org.openl.rules.commons.props.PropertiesContainer;

/**
 * Linked Entity...
 */
public interface LinkedEntity extends PropertiesContainer {
    /**
     * Gets an artefact to which the entity is linked to.
     *
     * @return linked artefact
     */
    Artefact getLinkedTo();

    /**
     * Links the entity to an artefact
     *
     * @param artefact artefact to link to
     */
    void linkTo(Artefact artefact);

    public void accept(LinkedEntityVisitor visitor);

}
