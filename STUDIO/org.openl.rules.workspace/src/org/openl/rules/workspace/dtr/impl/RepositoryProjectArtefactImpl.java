package org.openl.rules.workspace.dtr.impl;

import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.dtr.RepositoryProjectArtefact;
import org.openl.rules.workspace.props.PropertiesContainer;
import org.openl.rules.workspace.props.Property;
import org.openl.rules.workspace.props.PropertyException;
import org.openl.rules.workspace.props.PropertyTypeException;
import org.openl.rules.workspace.props.impl.PropertiesContainerImpl;

import java.util.Collection;
import java.util.LinkedList;

public abstract class RepositoryProjectArtefactImpl implements RepositoryProjectArtefact {
    private String name;
    private ArtefactPath path;
    private PropertiesContainer properties;

    protected RepositoryProjectArtefactImpl(String name, ArtefactPath path) {
        this.name = name;
        this.path = path;
        properties = new PropertiesContainerImpl();
    }

    public String getName() {
        return name;
    }

    public ArtefactPath getArtefactPath() {
        return path;
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
        throw new PropertyTypeException("Not supported");
    }

    public Property removeProperty(String name) throws PropertyException {
        throw new PropertyException("Not supported");
    }

    // all for project, main for content
    public Collection<ProjectVersion> getVersions() {
        // TODO -- add real code

        RepositoryVersionInfoImpl rvii = new RepositoryVersionInfoImpl(new java.util.Date(System.currentTimeMillis()), "user");
        Collection<ProjectVersion> pvs = new LinkedList<ProjectVersion>();
        pvs.add(new RepositoryProjectVersionImpl(0, 0, 1, rvii));

        return pvs;
    }
}
