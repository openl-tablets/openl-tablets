package org.openl.rules.webstudio.web;

/**
 * @author Andrei Astrouski
 */
public class ProjectHistoryItem {

    private long version;
    private String modifiedOn;

    public ProjectHistoryItem() {
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public String getModifiedOn() {
        return modifiedOn;
    }

    public void setModifiedOn(String modifiedOn) {
        this.modifiedOn = modifiedOn;
    }

}
