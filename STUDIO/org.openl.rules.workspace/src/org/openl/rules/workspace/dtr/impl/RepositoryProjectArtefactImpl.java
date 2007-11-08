package org.openl.rules.workspace.dtr.impl;

import org.openl.rules.repository.REntity;
import org.openl.rules.repository.RVersion;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.dtr.RepositoryProjectArtefact;
import org.openl.rules.workspace.props.PropertiesContainer;
import org.openl.rules.workspace.props.Property;
import org.openl.rules.workspace.props.PropertyException;
import org.openl.rules.workspace.props.PropertyTypeException;
import org.openl.rules.workspace.props.impl.PropertiesContainerImpl;
import org.openl.util.Log;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public abstract class RepositoryProjectArtefactImpl implements RepositoryProjectArtefact {
    private String name;
    private ArtefactPath path;
    private PropertiesContainer properties;
    private LinkedList<ProjectVersion> versions;

    protected RepositoryProjectArtefactImpl(REntity rulesEntity, ArtefactPath path) {
        this.name = rulesEntity.getName();
        this.path = path;
        
        versions = new LinkedList<ProjectVersion>();
        
        try {
            for (RVersion rv : rulesEntity.getVersionHistory()) {
                RepositoryVersionInfoImpl rvii = new RepositoryVersionInfoImpl(
                        rv.getCreated(), rv.getCreatedBy().getName());
                versions.add(new RepositoryProjectVersionImpl(rv.getMajor(), rv
                        .getMinor(), rv.getRevision(), rvii));
            }
        } catch (RRepositoryException e) {
            Log.error("Failed to get version history", e);
        }
        
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
        throw new PropertyTypeException("Not supported", null);
    }

    public Property removeProperty(String name) throws PropertyException {
        throw new PropertyException("Not supported", null);
    }

    // all for project, main for content
    public Collection<ProjectVersion> getVersions() {
        // TODO use updated each time
        return versions;
    }

    public void update(ProjectArtefact srcArtefact) throws ProjectException {
        // srcArtefact.getProperties();
        // TODO update properties
    }
}
