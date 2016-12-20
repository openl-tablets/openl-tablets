package org.openl.rules.repository;

import java.io.InputStream;
import java.util.List;

import org.openl.rules.repository.api.ArtefactAPI;
import org.openl.rules.repository.api.FolderAPI;
import org.openl.rules.repository.api.ResourceAPI;
import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * OpenL Rules Repository. A Repository can have any number of OpenL Rules
 * Projects.
 *
 * @author Aleh Bykhavets
 *
 */
public interface RRepository {

    boolean hasDeploymentProject(String name) throws RRepositoryException;

    /**
     * Checks whether project with given name exists in the repository.
     *
     * @param name
     * @return <code>true</code> if project with such name exists
     * @throws RRepositoryException
     */
    boolean hasProject(String name) throws RRepositoryException;

    /**
     * Releases resources allocated by this Rules Repository instance.
     */
    void release();


    //TODO new method names and comments
    
    
    FolderAPI createDeploymentProject(String name) throws RRepositoryException;

    /**
     * Creates a project in the repository. Name of new project must be unique.
     *
     * @param name name of new project
     * @return newly created project
     * @throws RRepositoryException if failed
     */
    FolderAPI createRulesProject(String name) throws RRepositoryException;

    FolderAPI getDeploymentProject(String name) throws RRepositoryException;

    List<FolderAPI> getDeploymentProjects() throws RRepositoryException;

    String getDeploymentConfigRootPath() throws RRepositoryException;
    String getDeploymentsRootPath() throws RRepositoryException;

    /**
     * Gets project by name.
     *
     * @param name
     * @return project
     * @throws RRepositoryException if failed or no project with specified name
     */
    FolderAPI getRulesProject(String name) throws RRepositoryException;

    /**
     * Gets list of projects from the repository.
     *
     * @return list of projects
     * @throws RRepositoryException if failed
     */
    List<FolderAPI> getRulesProjects() throws RRepositoryException;

    String getRulesProjectsRootPath() throws RRepositoryException;

    void addRepositoryListener(RRepositoryListener listener);
    void removeRepositoryListener(RRepositoryListener listener);
    ArtefactAPI getArtefact(String name) throws RRepositoryException;

    ResourceAPI createResource(String name, InputStream inputStream) throws RRepositoryException;

    ArtefactAPI rename(String path, String destination) throws RRepositoryException;
}
