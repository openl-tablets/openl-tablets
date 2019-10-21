package org.openl.rules.repository;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.openl.rules.common.CommonUser;
import org.openl.rules.common.Property;
import org.openl.rules.common.ValueType;
import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * Abstract Entity. It defines common properties and methods for OpenL Rules Project/Folder/File.
 *
 * @author Aleh Bykhavets
 *
 */
public interface REntity {

    void addProperty(String name, ValueType type, Object value) throws RRepositoryException;

    /**
     * Deletes entity. Also can delete other entities. For example, deleting a folder will lead to deleting all its sub
     * entities.
     *
     * @throws RRepositoryException if failed
     */
    void delete() throws RRepositoryException;

    /**
     * Gets active version of the entity.
     *
     * @return active version
     */
    RVersion getActiveVersion();

    /**
     * Gets effective date for rules entity. If effective date is not set method returns <code>null</code>
     *
     * @return effective date or <code>null</code>
     */
    Date getEffectiveDate();

    /**
     * Gets expiration date for rules entity. If expiration date is not set method returns <code>null</code>
     *
     * @return expiration date or <code>null</code>
     */
    Date getExpirationDate();

    /**
     * Gets line of business for rules entity. If line of business is not set method returns <code>null</code>
     *
     * @return line of business or <code>null</code>
     */
    String getLineOfBusiness();

    /**
     * Gets name of the entity.
     *
     * @return name
     */
    String getName();

    /**
     * Returns path of entity.
     *
     * @return path of entity
     * @throws RRepositoryException if failed
     */
    String getPath() throws RRepositoryException;

    Collection<Property> getProperties();

    Property getProperty(String name) throws RRepositoryException;

    Map<String, Object> getProps();

    /**
     * Gets version history of the entity.
     *
     * @return list of versions
     */
    List<RVersion> getVersionHistory() throws RRepositoryException;

    boolean hasProperty(String name);

    void removeProperty(String name) throws RRepositoryException;

    /**
     * Sets effective date for rules entity. Effective date can be disabled if <code>null</code> is passed.
     *
     * @param date new effective date or <code>null</code>
     * @throws RRepositoryException if failed
     */
    void setEffectiveDate(Date date) throws RRepositoryException;

    /**
     * Sets expiration date for rules entity. expiration date can be disabled if <code>null</code> is passed.
     *
     * @param date new expiration date or <code>null</code>
     * @throws RRepositoryException if failed
     */
    void setExpirationDate(Date date) throws RRepositoryException;

    /**
     * Sets line of business for rules entity. Line of business can be disabled if <code>null</code> is passed.
     *
     * @throws RRepositoryException if failed
     */
    void setLineOfBusiness(String lineOfBusiness) throws RRepositoryException;

    void setProps(Map<String, Object> props) throws RRepositoryException;

    RLock getLock() throws RRepositoryException;

    boolean isLocked() throws RRepositoryException;

    void lock(CommonUser user) throws RRepositoryException;

    void unlock(CommonUser user) throws RRepositoryException;

    /**
     * Commits changes in background versioned storage.
     *
     * @throws RRepositoryException if failed
     */
    void commit(CommonUser user) throws RRepositoryException;
}
