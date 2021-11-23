package org.openl.rules.rest.model;

import javax.validation.constraints.Size;

public class GroupSettingsModel {

    @Size(max = 50)
    private String defaultGroup;

    public String getDefaultGroup() {
        return defaultGroup;
    }

    public void setDefaultGroup(String defaultGroup) {
        this.defaultGroup = defaultGroup;
    }
}
