package org.openl.rules.repository.jcr;

import java.util.List;

import javax.jcr.RepositoryException;

/**
 * Defines JCR Folder.
 * A Folder can contain other folders (sub folders) and files.
 * 
 * @author Aleh Bykhavets
 *
 */
public interface JcrFolder extends JcrEntity, JcrVersionable {
	/**
	 * Returns list of sub/child folders.
	 * 
	 * @return list of sub folders
	 * @throws RepositoryException
	 */
	public List<JcrFolder> listSubFolders() throws RepositoryException;
	
	/**
	 * Retursn list of files within the folder.
	 * 
	 * @return list of files
	 * @throws RepositoryException
	 */
	public List<JcrFile> listFiles() throws RepositoryException;
	
	/**
	 * Checks whether the folder has sub folder with given name.
	 * 
	 * @param name name of sub folder to be checked
	 * @return <code>true</code> if sub folder with such <code>name</code> exists;
	 *         <code>false</code> otherwise.
	 * @throws RepositoryException
	 */
	public boolean hasSubFolder(String name) throws RepositoryException;
	
	/**
	 * Gets sub folder by given name.
	 * 
	 * @param name name of sub folder to be returned
	 * @return reference on sub folder instance
	 * @throws RepositoryException
	 */
	public JcrFolder getSubFolder(String name) throws RepositoryException;
	
	/**
	 * Checks whether the folder has file with given name.
	 * 
	 * @param name name of file to be checked
	 * @return <code>true</code> if the folder has such file;
	 *         <code>false</code> otherwise.
	 * @throws RepositoryException
	 */
	public boolean hasFile(String name) throws RepositoryException;
	
	/**
	 * Returns file by given name.
	 * 
	 * @param name name of file to be returned
	 * @return reference on file instance
	 * @throws RepositoryException
	 */
	public JcrFile getFile(String name) throws RepositoryException;
	
	/**
	 * Creates sub folder.
	 * 
	 * @param name name of sub folder
	 * @return newly created sub folder
	 * @throws RepositoryException if fails
	 */
	public JcrFolder createSubFolder(String name) throws RepositoryException;

	/**
	 * Creates file.
	 * 
	 * @param name name of file
	 * @return newly created file
	 * @throws RepositoryException if fails
	 */
	public JcrFile createFile(String name) throws RepositoryException;
}
