package org.openl.rules.repository;

import java.util.List;

import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * OpenL Rules Folder. It can have sub folders and files. Sub folders and files are treated separately.
 *
 * @author Aleh Bykhavets
 *
 */
public interface RFolder extends REntity {

    /**
     * Creates file to the folder.
     *
     * @param name name of new file
     * @return newly created file
     * @throws RRepositoryException if failed
     */
    RFile createFile(String name) throws RRepositoryException;

    /**
     * Creates sub folder to the folder.
     *
     * @param name name of new folder
     * @return newly created folder
     * @throws RRepositoryException if failed
     */
    RFolder createFolder(String name) throws RRepositoryException;

    /**
     * Deletes the folder, sub folders and all files.
     * <p>
     * Root folder cannot be deleted. Still, on delete it removes all its content, i.e. sub folders and all files.
     *
     * @throws RRepositoryException
     */
    @Override
    void delete() throws RRepositoryException;

    /**
     * Gets list of files from the folder.
     *
     * @return list of files
     */
    List<RFile> getFiles() throws RRepositoryException;

    /**
     * Gets list of sub folders. It returns direct descendants only.
     *
     * @return list of sub folders.
     */
    List<RFolder> getFolders() throws RRepositoryException;

}
