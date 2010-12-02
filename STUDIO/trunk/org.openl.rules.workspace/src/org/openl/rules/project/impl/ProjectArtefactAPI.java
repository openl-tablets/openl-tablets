package org.openl.rules.project.impl;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import org.openl.rules.repository.CommonUser;
import org.openl.rules.repository.CommonVersion;
import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.ProjectDependency;
import org.openl.rules.workspace.abstracts.ProjectDescriptor;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.dtr.LockInfo;
import org.openl.rules.workspace.props.Property;
import org.openl.rules.workspace.props.PropertyException;
import org.openl.rules.workspace.repository.RulesRepositoryArtefact;

public interface ProjectArtefactAPI extends RulesRepositoryArtefact{
    /**
     * Adds property into the container.
     *
     * @param property adding property
     * @throws PropertyException if property with the same name exists already
     *             and value cannot be updated.
     */
    void addProperty(Property property) throws PropertyException;

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

    void delete(CommonUser user) throws ProjectException;

    ProjectArtefactAPI getArtefact(String name) throws ProjectException;

    ArtefactPath getArtefactPath();

    String getName();

    boolean hasArtefact(String name);

    boolean isFolder();

    String getResourceType();

    InputStream getContent() throws ProjectException;
    void setContent(InputStream inputStream) throws ProjectException;

    ProjectArtefactAPI addFolder(String name) throws ProjectException;

    ProjectArtefactAPI addResource(String name, ProjectArtefactAPI resource) throws ProjectException;
    ProjectArtefactAPI addResource(String name, InputStream content) throws ProjectException;

    Collection<? extends ProjectArtefactAPI> getArtefacts();

    Collection<ProjectDependency> getDependencies();

    // current version
    ProjectVersion getVersion();

    void setDependencies(Collection<ProjectDependency> dependencies) throws ProjectException;

    void commit(CommonUser user,int major, int minor) throws ProjectException;

    void close(CommonUser user) throws ProjectException;

    LockInfo getLockInfo();
    
    void lock(CommonUser user) throws ProjectException;

    void unlock(CommonUser user) throws ProjectException;

    List<ProjectVersion> getVersions();

    /** no such project in DTR */
    boolean isLocalOnly();

    /** is opened by me? -- in LW */
    boolean isOpened();

    void openVersion(CommonVersion version) throws ProjectException;
    
    Collection<ProjectDescriptor> getProjectDescriptors();

    void setProjectDescriptors(Collection<ProjectDescriptor> projectDescriptors) throws ProjectException;
}
