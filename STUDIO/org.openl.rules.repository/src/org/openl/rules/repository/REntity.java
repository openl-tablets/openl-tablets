package org.openl.rules.repository;

import org.openl.rules.repository.exceptions.RDeleteException;
import org.openl.rules.repository.exceptions.RModifyException;
import org.openl.rules.repository.exceptions.RRepositoryException;

import java.util.List;

/**
 * Abstract Entity.
 * It defines common properties and methods for OpenL Rules Project/Folder/File.
 *
 * @author Aleh Bykhavets
 *
 */
public interface REntity {
    /**
     * Gets name of the entity.
     *
     * @return name
     */
    public String getName();

    /**
     * Gets base version of the entity.
     * Base version is currently active one.
     *
     * @return base version
     */
    public RVersion getBaseVersion();

    /**
     * Gets version history of the entity.
     *
     * @return list of versions
     */
    public List<RVersion> getVersionHistory() throws RRepositoryException;

    /**
     * Deletes entity.
     * Also can delete other entities.
     * For example, deleting a folder will lead to deleting all its sub entities.
     *
     * @throws RDeleteException if failed
     */
    public void delete() throws RDeleteException;

    /**
     * Sets/Updates name of the entity.
     *
     * @param name new name
     * @throws RModifyException if failed
     */
    public void setName(String name) throws RModifyException;

    /**
     * Returns path of entity.
     *
     * @return path of entity
     * @throws RRepositoryException if failed
     */
    public String getPath() throws RRepositoryException;
}
