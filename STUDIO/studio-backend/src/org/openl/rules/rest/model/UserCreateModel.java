package org.openl.rules.rest.model;

import java.util.Set;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;

import org.openl.rules.rest.validation.InternalPasswordConstraint;
import org.openl.rules.rest.validation.UsernameExistsConstraint;
import org.openl.rules.security.standalone.persistence.UsernameConstraints;

public class UserCreateModel extends UserEditModel {

    @Getter
    @NotBlank
    @Size(max = 25, message = "{openl.constraints.size.max.message}")
    @UsernameConstraints
    @UsernameExistsConstraint
    @Parameter(description = "Username", example = "jdoe")
    private String username;

    @Getter
    @InternalPasswordConstraint
    private InternalPasswordModel internalPassword;

    public UserCreateModel setUsername(String username) {
        this.username = username;
        return this;
    }

    public UserCreateModel setInternalPassword(InternalPasswordModel internalPassword) {
        this.internalPassword = internalPassword;
        return this;
    }

    @Override
    public UserCreateModel setFirstName(String firstName) {
        return (UserCreateModel) super.setFirstName(firstName);
    }

    @Override
    public UserCreateModel setLastName(String lastName) {
        return (UserCreateModel) super.setLastName(lastName);
    }

    @Override
    public UserCreateModel setEmail(String email) {
        return (UserCreateModel) super.setEmail(email);
    }

    @Override
    public UserCreateModel setDisplayName(String displayName) {
        return (UserCreateModel) super.setDisplayName(displayName);
    }

    @Override
    public UserCreateModel setGroups(Set<String> groups) {
        return (UserCreateModel) super.setGroups(groups);
    }

}
