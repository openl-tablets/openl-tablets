package org.openl.rules.rest.model;

import jakarta.validation.constraints.NotBlank;

public class UserInfoEditModel extends UserInfoModel {

    @Override
    @NotBlank
    public String getEmail() {
        return super.getEmail();
    }

    @Override
    public UserInfoEditModel setEmail(String email) {
        super.setEmail(email);
        return this;
    }

    @Override
    @NotBlank
    public String getDisplayName() {
        return super.getDisplayName();
    }

    @Override
    public UserInfoEditModel setDisplayName(String displayName) {
        super.setDisplayName(displayName);
        return this;
    }

    @Override
    public UserInfoEditModel setFirstName(String firstName) {
        super.setFirstName(firstName);
        return this;
    }

    @Override
    public UserInfoEditModel setLastName(String lastName) {
        super.setLastName(lastName);
        return this;
    }
}
