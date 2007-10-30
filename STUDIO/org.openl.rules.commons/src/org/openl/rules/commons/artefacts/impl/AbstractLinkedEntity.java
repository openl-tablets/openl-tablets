package org.openl.rules.commons.artefacts.impl;

import org.openl.rules.commons.artefacts.LinkedEntity;
import org.openl.rules.commons.artefacts.Artefact;
import org.openl.rules.commons.artefacts.LinkedEntityVisitor;
import org.openl.rules.commons.props.PropertiesContainer;
import org.openl.rules.commons.props.Property;
import org.openl.rules.commons.props.PropertyException;
import org.openl.rules.commons.props.PropertyTypeException;
import org.openl.rules.commons.props.impl.PropertiesContainerImpl;

import java.util.Collection;

public abstract class AbstractLinkedEntity implements LinkedEntity {
    private Artefact artefact;
    private PropertiesContainer properties;

    protected AbstractLinkedEntity(Artefact artefact) {
        this.artefact = artefact;
        properties = new PropertiesContainerImpl();
    }

    public Artefact getLinkedTo() {
        return artefact;
    }

    public void linkTo(Artefact artefact) {
        this.artefact = artefact;
    }

    public void accept(LinkedEntityVisitor visitor) {
        visitor.visit(this);
    }

    public boolean hasProperty(String name) {
        return properties.hasProperty(name);
    }

    public Property getProperty(String name) throws PropertyException {
        return properties.getProperty(name);
    }

    public Collection<Property> getProperties() {
        return properties.getProperties();
    }

    public void addProperty(Property property) throws PropertyTypeException {
        properties.addProperty(property);
    }

    public Property removeProperty(String name) throws PropertyException {
        return properties.removeProperty(name);
    }
}
