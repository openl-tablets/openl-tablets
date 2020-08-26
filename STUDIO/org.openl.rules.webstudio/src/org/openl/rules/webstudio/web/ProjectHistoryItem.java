package org.openl.rules.webstudio.web;

public class ProjectHistoryItem {

    private final long version;
    private final String modifiedOn;

    public ProjectHistoryItem(long version, String modifiedOn) {
        this.version = version;
        this.modifiedOn = modifiedOn;
    }

    public long getVersion() {
        return version;
    }

    public String getModifiedOn() {
        return modifiedOn;
    }
}
