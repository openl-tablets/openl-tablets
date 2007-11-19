package org.openl.rules.workspace.mock;

import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.props.Property;
import org.openl.rules.workspace.props.PropertyException;

import java.util.Collection;
import java.util.Collections;

public class MockArtefact implements ProjectArtefact{

    private final static PropertyException PROPERTY_EXCEPTION = new PropertyException("", null);
    private String name;
    private MockFolder parent;

    public MockArtefact(String name, MockFolder parent) {
        this.name = name;
        this.parent = parent;
    }

    public MockFolder up() {
        return parent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArtefactPath getArtefactPath() {
        return null;
    }

    public ProjectArtefact getArtefact(String name) throws ProjectException {
        return null;
    }

    public boolean hasArtefact(String name) {
        return false;
    }

    public boolean isFolder() {
        return false;
    }

    /**
     * Checks whether property with specified name exists in the container.
     *
     * @param name name of property
     * @return <code>true</code> if such property exists
     */
    public boolean hasProperty(String name) {
        return false;
    }

    /**
     * Returns property by name.
     *
     * @param name name of property
     * @return reference on named property
     * @throws org.openl.rules.workspace.props.PropertyException
     *          if no property with specified name
     */
    public Property getProperty(String name) throws PropertyException {
        throw PROPERTY_EXCEPTION;
    }

    /**
     * Gets list of all properties in the container.
     *
     * @return list of properties
     */
    public Collection<Property> getProperties() {
        return Collections.emptyList();
    }

    /**
     * Adds property into the container.
     *
     * @param property adding property
     * @throws org.openl.rules.workspace.props.PropertyException
     *          if property with the same name exists already and value cannot be updated.
     */
    public void addProperty(Property property) throws PropertyException {
    
    }

    /**
     * Removes property from the container.
     *
     * @param name name of property
     * @return removed property
     * @throws org.openl.rules.workspace.props.PropertyException
     *          if no property with specified name
     */
    public Property removeProperty(String name) throws PropertyException {
        throw PROPERTY_EXCEPTION;
    }
}
