package org.openl.rules.ui.repository.beans;

import java.util.Date;
import java.util.List;

/**
 * Entity interface for UI Beans.
 * 
 * @author Aleh Bykhavets
 *
 */
public interface Entity {

    /**
     * Gets name of the entity.
     * 
     * @return name
     */
    public String getName();

    /**
     * Gets version name of the entity
     * 
     * @return name of version
     */
    public String getVersion();

    /**
     * Gets date/time when the entity was modified last time.
     * 
     * @return date of last modification
     */
    public Date getLastModified();

    /**
     * Gets name of user who modified the entity last time.
     * 
     * @return name of user
     */
    public String getLastModifiedBy();

    /**
     * Gets version history of the entity
     * 
     * @return history of versions
     */
    public List<VersionBean> getVersionHistory();

    /**
     * Gets date/time when the entity was created.
     * 
     * @return date of creation
     */
    public Date getCreated();

    /**
     * Gets list of elements of the entity.
     * I.e. list of folders and files.
     * 
     * @return list of elements
     */
    public List<AbstractEntityBean> getElements();

    /**
     * Deletes entity.
     * UI Bean do not deletes the entity itself.
     * Instead it delegates it to own handler.
     */
    public void delete();

}