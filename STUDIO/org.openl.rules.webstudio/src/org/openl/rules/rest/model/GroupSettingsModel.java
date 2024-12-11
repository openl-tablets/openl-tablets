package org.openl.rules.rest.model;

import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.Parameter;

@Deprecated(forRemoval = true)
public class GroupSettingsModel {

    @Size(max = 65)
    @Parameter(description = "Default user group")
    private String defaultGroup;

    public String getDefaultGroup() {
        return defaultGroup;
    }

    public void setDefaultGroup(String defaultGroup) {
        this.defaultGroup = defaultGroup;
    }
}
