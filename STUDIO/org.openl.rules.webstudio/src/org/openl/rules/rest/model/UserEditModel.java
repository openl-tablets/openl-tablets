package org.openl.rules.rest.model;

import java.util.Set;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.Parameter;

public class UserEditModel extends UserInfoModel {

    @Size(max = 25, message = "{openl.constraints.size.max.message}")
    @Parameter(description = "Password", example = "qwerty")
    private String password;

    @Parameter(description = "Assigned Groups")
    private Set<String> groups;

    @Override
    public UserEditModel setEmail(String email) {
        return (UserEditModel) super.setEmail(email);
    }

    @Override
    public UserEditModel setDisplayName(String displayName) {
        return (UserEditModel) super.setDisplayName(displayName);
    }

    @Override
    public UserEditModel setFirstName(String firstName) {
        return (UserEditModel) super.setFirstName(firstName);
    }

    @Override
    public UserEditModel setLastName(String lastName) {
        return (UserEditModel) super.setLastName(lastName);
    }

    public String getPassword() {
        return password;
    }

    public UserEditModel setPassword(String password) {
        this.password = password;
        return this;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public UserEditModel setGroups(Set<String> groups) {
        this.groups = groups;
        return this;
    }
}
