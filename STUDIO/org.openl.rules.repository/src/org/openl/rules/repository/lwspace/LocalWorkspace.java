package org.openl.rules.repository.lwspace;

import java.io.File;

import org.openl.rules.repository.RProject;
import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * Local Workspace defines a place where OpenL project files
 * are stored when Web Studio works with them.
 * <p>
 * OpenL will load files and classes from Local Workspace later.
 * Thus, temporary folder should be recognized by class loader.
 * 
 * @author Aleh Bykhavets
 *
 */
public interface LocalWorkspace {
    /**
     * Initializes local workspace.
     * At first it cleans temporary folder from leftovers, if any.
     * Then it downloads files from JCR.
     *
     * @param project JCR project
     * @param tempLocation temporary folder for project files
     */
    public void initialize(RProject project, File tempLocation) throws RRepositoryException;

    /**
     * Cleans the local workspace.
     * All files in temporary folder will be deleted.
     */
    public void clean();

    /**
     * Reverts all changes in the Local Workspace.
     * All changes will be lost.
     *
     * @throws RRepositoryException
     */
    public void revert() throws RRepositoryException;

    /**
     * Commits all changes to JCR
     *
     * @throws RRepositoryException
     */
    public void commit() throws RRepositoryException;
}
