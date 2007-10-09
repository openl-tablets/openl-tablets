package org.openl.rules.ui.repository.beans;

import org.openl.rules.ui.repository.handlers.BeanHandler;

import java.util.List;
import java.util.Date;

public abstract class AbstractEntityBean implements Entity {
    private String id;
    private BeanHandler handler;

    private String name;
    private String version;
    private Date created;
    private Date lastModified;
    private String lastModifiedBy;

    // ------ system

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setHandler(BeanHandler handler) {
        this.handler = handler;
    }

    protected BeanHandler getHandler() {
        return handler;
    }

    // ------ properties ------

    /* (non-Javadoc)
     * @see org.openl.rules.ui.repository.beans.Entity#getName()
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /* (non-Javadoc)
     * @see org.openl.rules.ui.repository.beans.Entity#getVersion()
     */
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /* (non-Javadoc)
     * @see org.openl.rules.ui.repository.beans.Entity#getLastModified()
     */
    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    /* (non-Javadoc)
     * @see org.openl.rules.ui.repository.beans.Entity#getLastModifiedBy()
     */
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    /* (non-Javadoc)
     * @see org.openl.rules.ui.repository.beans.Entity#getVersionHistory()
     */
    public List<VersionBean> getVersionHistory() {
        return handler.getVersions(this);
    }

    /* (non-Javadoc)
     * @see org.openl.rules.ui.repository.beans.Entity#getCreated()
     */
    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    /* (non-Javadoc)
     * @see org.openl.rules.ui.repository.beans.Entity#getElements()
     */
    public abstract List<AbstractEntityBean> getElements();
    
    // ------ actions ------

    /* (non-Javadoc)
     * @see org.openl.rules.ui.repository.beans.Entity#delete()
     */
    public void delete() {
        System.out.println("? delete(): " + id);
        handler.delete(this);
    }
}
