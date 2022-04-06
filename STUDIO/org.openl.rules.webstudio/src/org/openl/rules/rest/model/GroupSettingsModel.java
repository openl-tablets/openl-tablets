package org.openl.rules.rest.model;

import javax.validation.constraints.Size;

import io.swagger.v3.oas.annotations.Parameter;

public class GroupSettingsModel {

    @Size(max = 50)
    @Parameter(description = "Default user group")
    private String defaultGroup;

    public String getDefaultGroup() {
        return defaultGroup;
    }

    public void setDefaultGroup(String defaultGroup) {
        this.defaultGroup = defaultGroup;
    }
}
