package org.openl.rules.repository;

import java.util.List;

import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * OpenL Rules Repository.
 * A Repository can have any number of OpenL Rules Projects.
 *
 * @author Aleh Bykhavets
 *
 */
public interface RRepository {
    /**
     * Gets list of projects from the repository.
     *
     * @return list of projects
     * @throws RRepositoryException if failed
     */
    public List<RProject> getProjects() throws RRepositoryException;

    /**
     * Gets list of projects from the repository that are marked for deletion.
     *
     * @return list of projects that are marked for deletion
     */
    public List<RProject> getProjects4Deletion() throws RRepositoryException;
    
    /**
     * Creates a project in the repository.
     * Name of new project must be unique.
     * 
     * @param name name of new project
     * @return newly created project
     * @throws RRepositoryException if failed
     */
    public RProject createProject(String name) throws RRepositoryException;

    /**
     * Releases resources allocated by this JcrRepository instance.
     */
    public void release();
}
