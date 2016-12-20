package org.openl.rules.repository;

import java.io.InputStream;

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

    void addRepositoryListener(RRepositoryListener listener);
    void removeRepositoryListener(RRepositoryListener listener);
    ArtefactAPI getArtefact(String name) throws RRepositoryException;

    ResourceAPI createResource(String name, InputStream inputStream) throws RRepositoryException;

    ArtefactAPI rename(String path, String destination) throws RRepositoryException;
}
