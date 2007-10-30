package org.openl.rules.commons.artefacts.impl;

import org.openl.rules.commons.artefacts.*;

import java.util.Collection;
import java.util.TreeMap;

/**
 * Implementation of Artefact
 */
public class ArtefactImpl implements Artefact {
    private String name;
    private Artefact parent;
    private TreeMap<String, Artefact> children;
    private LinkedEntity linkedEntity;

    public ArtefactImpl(String name, Artefact parent) throws ArtefactException {
        this.name = name;
        this.parent = parent;

        if (parent != null) {
            parent.addArtefact(this);
        }

        children = new TreeMap<String, Artefact>();
    }

    /** {@inheritDoc} */
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    public Artefact getParent() {
        return parent;
    }

    /** {@inheritDoc} */
    public Project getProject() {
        Artefact artefact = this;

        while (artefact != null) {
            if (artefact instanceof Project) break;

            artefact = artefact.getParent();
        }

        //TODO if (artefact == null) --> error

        return (Project) artefact;
    }

    /** {@inheritDoc} */
    public ArtefactPath getArtefactPath() throws ArtefactException {
        return getProject().getArtefactPath(this);
    }

    @Override
    public int hashCode() {
        int thisHashCode = getName().hashCode();

        if (parent == null) {
            return thisHashCode;
        } else {
            return ((thisHashCode >> 3) & 0x1FFFFFFF | (thisHashCode << 29) & 0xE0000000) + parent.hashCode();
        }
    }

    /** {@inheritDoc} */
    public void accept(ArtefactVisitor visitor) throws ArtefactException {
        if (visitor.visit(this)) {
            for (Artefact a : children.values()) {
                a.accept(visitor);
            }
        }
    }

    /** {@inheritDoc} */
    public boolean isLeaf() {
        return !children.isEmpty();
    }

    /** {@inheritDoc} */
    public boolean hasArtefact(String name) {
        Artefact a = children.get(name);
        return (a != null);
    }

    /** {@inheritDoc} */
    public Artefact getArtefact(String name) throws ArtefactException {
        Artefact a = children.get(name);

        if (a == null) {
            throw new ArtefactException("Cannot find artefact ''{0}''", name);
        }

        return a;
    }

    /** {@inheritDoc} */
    public Collection<Artefact> getArtefacts() throws ArtefactException {
        return children.values();
    }

    /** {@inheritDoc} */
    public LinkedEntity getLinkedEntity() {
        return linkedEntity;
    }

    /** {@inheritDoc} */
    public void accept(LinkedEntityVisitor visitor) throws ArtefactException {
        if (visitor.visit(getLinkedEntity())) {
            for (Artefact a : children.values()) {
                a.accept(visitor);
            }

            visitor.afterVisit(getLinkedEntity());
        }
    }

    /** {@inheritDoc} */
    public void setLinkedEntity(LinkedEntity linkedEntity) {
        this.linkedEntity = linkedEntity;
    }

    /** {@inheritDoc} */
    public void addArtefact(Artefact artefact) throws ArtefactException {
        // artefact != null
        String name = artefact.getName();
        if (hasArtefact(name)) {
            throw new ArtefactException("Artefact with name ''{0}'' is in composition already", name);
        }

        children.put(name, artefact);
    }

    /** {@inheritDoc} */
    public Artefact removeArtefact(String name) throws ArtefactException {
        // throws exception if there is no artefact with such name
        Artefact a = getArtefact(name);

        children.remove(name);
        // a.getParent() is still referencing on a valid artefact
        return a;
    }

    /** {@inheritDoc} */
    public void delete() throws ArtefactException {
        deleteChildren();
        deleteItself();
    }

    // --- protected

    protected void deleteChildren() throws ArtefactException {
        for (Artefact a : getArtefacts()) {
            ArtefactImpl ai = (ArtefactImpl) a;
            ai.delete();
        }

        children.clear();
    }

    protected void deleteItself() throws ArtefactException {
        parent = null;
        linkedEntity = null;
    }
}
