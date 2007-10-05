package org.openl.rules.ui.repository.beans;

import org.openl.rules.ui.repository.handlers.BeanHandler;

import java.util.List;
import java.util.Date;

public abstract class AbstractEntityBean {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public List<VersionBean> getVersionHistory() {
        return handler.getVersions(this);
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public abstract List<AbstractEntityBean> getElements();
    
    // ------ actions ------

    public void delete() {
        System.out.println("? delete(): " + id);
        handler.delete(this);
    }
}
