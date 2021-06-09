package org.openl.rules.rest;

public class ProjectHistoryItem {

    public final String id;
    public final String modifiedOn;
    public final Boolean current;

    ProjectHistoryItem(String id, String modifiedOn, boolean current) {
        this.current = current ? true : null; // null - is to reduce payload on ~27% - 16 bytes per item
        this.id = id;
        this.modifiedOn = modifiedOn;
    }
}
