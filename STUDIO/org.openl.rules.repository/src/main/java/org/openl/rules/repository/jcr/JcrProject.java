package org.openl.rules.repository.jcr;

import javax.jcr.RepositoryException;

/**
 * Defines JCR Project.
 * 
 * @author Aleh Bykhavets
 *
 */
public interface JcrProject extends JcrEntity, JcrVersionable {
	/**
	 * Returns root folder of the project.
	 * 
	 * @return reference on root folder.
	 */
	public JcrFolder getRootFolder();
	
	/**
	 * Inquires whether the project is marked for deletion.
	 * <p>
	 * When a project is marked it should not be used.
	 * 
	 * @return <code>true</code> the project is going to be deleted; 
	 *         <code>false</code> the project is alive.
	 * @throws RepositoryException if operation fails
	 */
	public boolean isMarked4Deletion() throws RepositoryException;
	
	/**
	 * Marks project for deletion.
	 * Later administrator can erase such projects from the Repository.
	 * <p>
	 * Note, after erasing a project cannot be restored. 
	 * 
	 * @throws RepositoryException if operation fails
	 */
	public void mark4deletion() throws RepositoryException;
	
	/**
	 * Remove deletion mark from the project.
	 * After that it can be used again.
	 * 
	 * @throws RepositoryException if operation fails
	 */
	public void unmark4deletion() throws RepositoryException;
}
