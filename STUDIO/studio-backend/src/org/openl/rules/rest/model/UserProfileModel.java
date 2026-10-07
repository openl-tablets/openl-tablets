package org.openl.rules.rest.model;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;

import org.openl.rules.security.UserExternalFlags;

public class UserProfileModel extends UserProfileBaseModel {

    @Getter
    @Parameter(description = "Username")
    private String username;

    @Getter
    private UserExternalFlags externalFlags;

    @Getter
    private boolean administrator;

    public UserProfileModel setUsername(String username) {
        this.username = username;
        return this;
    }

    public UserProfileModel setExternalFlags(UserExternalFlags externalFlags) {
        this.externalFlags = externalFlags;
        return this;
    }

    @Override
    public UserProfileModel setFirstName(String firstName) {
        return (UserProfileModel) super.setFirstName(firstName);
    }

    @Override
    public UserProfileModel setLastName(String lastName) {
        return (UserProfileModel) super.setLastName(lastName);
    }

    @Override
    public UserProfileModel setEmail(String email) {
        return (UserProfileModel) super.setEmail(email);
    }

    @Override
    public UserProfileModel setDisplayName(String displayName) {
        return (UserProfileModel) super.setDisplayName(displayName);
    }

    public UserProfileModel setAdministrator(boolean administrator) {
        this.administrator = administrator;
        return this;
    }
}
