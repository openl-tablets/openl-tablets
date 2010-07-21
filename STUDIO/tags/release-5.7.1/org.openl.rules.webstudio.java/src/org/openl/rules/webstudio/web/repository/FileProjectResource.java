package org.openl.rules.webstudio.web.repository;

import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedList;

import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectResource;
import org.openl.rules.workspace.abstracts.impl.ArtefactPathImpl;
import org.openl.rules.workspace.props.Property;
import org.openl.rules.workspace.props.PropertyException;

/**
 * Base project resource implementation.
 *
 * @author Andrey Naumenko
 */
public class FileProjectResource implements ProjectResource {
    private InputStream is;

    public FileProjectResource(InputStream is) {
        this.is = is;
    }

    public void addProperty(Property property) throws PropertyException {
        throw new PropertyException("Not supported", null);
    }

    public ProjectArtefact getArtefact(String name) throws ProjectException {
        throw new ProjectException("Not supported", null);
    }

    public ArtefactPath getArtefactPath() {
        return new ArtefactPathImpl("/noname");
    }

    public InputStream getContent() throws ProjectException {
        return is;
    }

    public String getName() {
        return "noname";
    }

    public Collection<Property> getProperties() {
        return new LinkedList<Property>();
    }

    public Property getProperty(String name) throws PropertyException {
        throw new PropertyException("Not supported", null);
    }

    public String getResourceType() {
        return "file";
    }

    public boolean hasArtefact(String name) {
        return false;
    }

    public boolean hasProperty(String name) {
        return false;
    }

    public boolean isFolder() {
        return false;
    }

    public Property removeProperty(String name) throws PropertyException {
        throw new PropertyException("Not supported", null);
    }
}
