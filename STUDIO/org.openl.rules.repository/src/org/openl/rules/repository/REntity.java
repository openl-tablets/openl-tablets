package org.openl.rules.repository;

import org.openl.rules.repository.exceptions.RDeleteException;
import org.openl.rules.repository.exceptions.RModifyException;
import org.openl.rules.repository.exceptions.RRepositoryException;

import java.util.Collection;
import java.util.Date;
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
     * Returns path of entity.
     *
     * @return path of entity
     * @throws RRepositoryException if failed
     */
    public String getPath() throws RRepositoryException;
    
    /**
     * Gets effective date for rules entity.
     * If effective date isn't set method returns <code>null</code>
     * 
     * @return effective date or <code>null</code>
     */
    public Date getEffectiveDate();

    /**
     * Gets expiration date for rules entity.
     * If expiration date isn't set method returns <code>null</code>
     * 
     * @return expiration date or <code>null</code>
     */
    public Date getExpirationDate();

    /**
     * Gets line of business for rules entity.
     * If line of business isn't set method returns <code>null</code>
     * 
     * @return line of business or <code>null</code>
     */
    public String getLineOfBusiness();
    
    /**
     * Sets effective date for rules entity.
     * Effective date can be disabled if <code>null</code> is passed.
     * 
     * @param date new effective date or <code>null</code>
     * @throws RModifyException if failed
     */
    public void setEffectiveDate(Date date) throws RModifyException;

    /**
     * Sets expiration date for rules entity.
     * expiration date can be disabled if <code>null</code> is passed.
     * 
     * @param date new expiration date or <code>null</code>
     * @throws RModifyException if failed
     */
    public void setExpirationDate(Date date) throws RModifyException;

    /**
     * Sets line of business for rules entity.
     * Line of business can be disabled if <code>null</code> is passed.
     * 
     * @param date new line of business or <code>null</code>
     * @throws RModifyException if failed
     */
    public void setLineOfBusiness(String lineOfBusiness) throws RModifyException;
    
    public Collection<RProperty> getProperties();
    public void addProperty(String name, RPropertyType type, Object value) throws RRepositoryException;
    public void removeProperty(String name) throws RDeleteException;
    public boolean hasProperty(String name);
    public RProperty getProperty(String name) throws RRepositoryException;
}
