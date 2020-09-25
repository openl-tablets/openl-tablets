package org.openl.rules.rest;

public class ProjectHistoryItem {

    public final String id;
    public final String modifiedOn;
    public final String type;

    ProjectHistoryItem(String id, String modifiedOn, String type) {
        this.type = type;
        this.id = id;
        this.modifiedOn = modifiedOn;
    }
}
