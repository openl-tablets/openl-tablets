package org.openl.rules.repository.lwspace;

import java.io.File;

import javax.jcr.RepositoryException;

import org.openl.rules.repository.jcr.JcrProject;

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
	public void initialize(JcrProject project, File tempLocation) throws RepositoryException;

	/**
	 * Cleans the local workspace.
	 * All files in temporary folder will be deleted.  
	 */
	public void clean();
	
	/**
	 * Reverts all changes in the Local Workspace.
	 * All changes will be lost.
	 * 
	 * @throws RepositoryException
	 */
	public void revert() throws RepositoryException;
	
	/**
	 * Commits all changes to JCR
     *
	 * @throws RepositoryException
	 */
	public void commit() throws RepositoryException;
}
