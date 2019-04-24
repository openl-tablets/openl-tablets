package org.openl.rules.rest;

import java.util.Date;

/**
 * A project description bean.
 * 
 * @author Yury Molchan
 */
public class ProjectDescription {
    private String name; // A project name
    private String version; // Last version of the project
    private Date modifiedAt; // When was the project modified
    private String modifiedBy; // Who modified the project
    private boolean locked; // Is the project locked for editing?
    private String lockedBy; // Who locked the project for editing
    private Date lockedAt; // When locked the project for editing

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

    public Date getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(Date modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public void setLockedBy(String lockedBy) {
        this.lockedBy = lockedBy;
    }

    public Date getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(Date lockedAt) {
        this.lockedAt = lockedAt;
    }
}
