package org.openl.rules.repository;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * Abstract Entity. It defines common properties and methods for OpenL Rules
 * Project/Folder/File.
 *
 * @author Aleh Bykhavets
 *
 */
public interface REntity {
    public void addProperty(String name, RPropertyType type, Object value) throws RRepositoryException;

    /**
     * Deletes entity. Also can delete other entities. For example, deleting a
     * folder will lead to deleting all its sub entities.
     *
     * @throws RRepositoryException if failed
     */
    public void delete() throws RRepositoryException;

    /**
     * Gets active version of the entity.
     *
     * @return active version
     */
    public RVersion getActiveVersion();

    /**
     * Gets effective date for rules entity. If effective date isn't set method
     * returns <code>null</code>
     *
     * @return effective date or <code>null</code>
     */
    public Date getEffectiveDate();

    /**
     * Gets expiration date for rules entity. If expiration date isn't set
     * method returns <code>null</code>
     *
     * @return expiration date or <code>null</code>
     */
    public Date getExpirationDate();

    /**
     * Gets line of business for rules entity. If line of business isn't set
     * method returns <code>null</code>
     *
     * @return line of business or <code>null</code>
     */
    public String getLineOfBusiness();

    /**
     * Gets name of the entity.
     *
     * @return name
     */
    public String getName();

    /**
     * Returns path of entity.
     *
     * @return path of entity
     * @throws RRepositoryException if failed
     */
    public String getPath() throws RRepositoryException;

    public Collection<RProperty> getProperties();

    public RProperty getProperty(String name) throws RRepositoryException;

    public Map<String, Object> getProps();

    /**
     * Gets version history of the entity.
     *
     * @return list of versions
     */
    public List<RVersion> getVersionHistory() throws RRepositoryException;

    public boolean hasProperty(String name);

    public void removeProperty(String name) throws RRepositoryException;

    /**
     * Sets effective date for rules entity. Effective date can be disabled if
     * <code>null</code> is passed.
     *
     * @param date new effective date or <code>null</code>
     * @throws RRepositoryException if failed
     */
    public void setEffectiveDate(Date date) throws RRepositoryException;

    /**
     * Sets expiration date for rules entity. expiration date can be disabled if
     * <code>null</code> is passed.
     *
     * @param date new expiration date or <code>null</code>
     * @throws RRepositoryException if failed
     */
    public void setExpirationDate(Date date) throws RRepositoryException;

    /**
     * Sets line of business for rules entity. Line of business can be disabled
     * if <code>null</code> is passed.
     *
     * @throws RRepositoryException if failed
     */
    public void setLineOfBusiness(String lineOfBusiness) throws RRepositoryException;

    public void setProps(Map<String, Object> props) throws RRepositoryException;
}
