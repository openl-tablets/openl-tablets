package org.openl.rules.rest.model;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.Size;

public class GroupSettingsModel {

    @Size(max = 50)
    @Schema(description = "Default user group")
    private String defaultGroup;

    public String getDefaultGroup() {
        return defaultGroup;
    }

    public void setDefaultGroup(String defaultGroup) {
        this.defaultGroup = defaultGroup;
    }
}
