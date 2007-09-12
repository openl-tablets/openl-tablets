package org.openl.rules.repository.jcr;

import java.util.List;

import javax.jcr.RepositoryException;

/**
 * Defines JCR Repository.
 * 
 * @author Aleh Bykhavets
 *
 */
public interface JcrRepository {
	/**
	 * Lists OpenL projects in the repository.
	 * 
	 * @return list of available projects
	 * @throws RepositoryException
	 */
	public List<JcrProject> listProjects() throws RepositoryException;
	
	/**
	 * Lists OpenL projects that are going to be deleted.
	 * Any project from this list should not be revealed to users.
	 * <p>
	 * Only administrator can see those projects.
	 * He/She can decide to undelete such project or to erase it completely.
	 * 
	 * @return list of projects marked for deletion
	 * @throws RepositoryException
	 */
	public List<JcrProject> listProjects4Deletion() throws RepositoryException;
	
	/**
	 * Releases resources allocated by this JcrRepository instance.
	 */
	// TODO: do we need it?
	public void release();
	
	/**
	 * Creates project in the repository.
	 * <code>nodeName</code> must be unique in a scope of sibling nodes.
	 * 
	 * @param nodeName name of node
	 * @return newly created project
	 * @throws RepositoryException if fails
	 */
	public JcrProject createProject(String nodeName) throws RepositoryException;
}
