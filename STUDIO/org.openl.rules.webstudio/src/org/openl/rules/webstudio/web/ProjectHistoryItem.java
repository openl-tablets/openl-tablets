package org.openl.rules.webstudio.web;

/**
 * @author Andrei Astrouski
 */
public class ProjectHistoryItem {

    private long version;
    private String modifiedOn;
    private String sourceName;
    private boolean disabled;
    private boolean current;

    public ProjectHistoryItem(long version, String modifiedOn, String sourceName) {
        this.version = version;
        this.modifiedOn = modifiedOn;
        this.sourceName = sourceName;
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

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }
}
