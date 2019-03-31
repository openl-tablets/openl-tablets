package org.openl.rules.repository.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.CommonUser;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.InheritedProperty;
import org.openl.rules.common.LockInfo;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.Property;
import org.openl.rules.common.PropertyException;
import org.openl.rules.common.ValueType;
import org.openl.rules.repository.exceptions.RRepositoryException;

public interface ArtefactAPI {
    void addProperty(String name, ValueType type, Object value) throws PropertyException;

    /**
     * Gets list of all properties in the container.
     *
     * @return list of properties
     */
    Collection<Property> getProperties();

    /**
     * Returns property by name.
     *
     * @param name name of property
     * @return reference on named property
     * @throws PropertyException if no property with specified name
     */
    Property getProperty(String name) throws PropertyException;

    /**
     * Checks whether property with specified name exists in the container.
     *
     * @param name name of property
     * @return <code>true</code> if such property exists
     */
    boolean hasProperty(String name);

    /**
     * Removes property from the container.
     *
     * @param name name of property
     * @return removed property
     * @throws PropertyException if no property with specified name
     */
    Property removeProperty(String name) throws PropertyException;

    void removeAllProperties() throws PropertyException;

    void delete(CommonUser user) throws ProjectException;

    ArtefactPath getArtefactPath();

    String getName();

    boolean isFolder();

    // current version
    ProjectVersion getVersion();

    // TODO exception should be thrown if error occurs
    List<ProjectVersion> getVersions();

    // TODO exception should be thrown if error occurs
    int getVersionsCount();

    ProjectVersion getVersion(int index) throws RRepositoryException;

    LockInfo getLockInfo();

    void commit(CommonUser user, int revision) throws ProjectException;

    void lock(CommonUser user) throws ProjectException;

    void unlock(CommonUser user) throws ProjectException;

    ArtefactAPI getVersion(CommonVersion version) throws ProjectException;

    Map<String, Object> getProps();

    void setProps(Map<String, Object> props) throws PropertyException;

    Map<String, InheritedProperty> getInheritedProps();
}
