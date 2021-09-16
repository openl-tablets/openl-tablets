package org.openl.rules.rest.model;

import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.Email;

public class UserInfoModel {

    @Email(message = "{openl.constraints.user.email.format.message}")
    @Size(max = 254, message = "{openl.constraints.user.email.max-length.message}")
    private String email;

    @Size(max = 64, message = "{openl.constraints.user.display-name.max-length.message}")
    private String displayName;

    @Size(max = 25, message = "{openl.constraints.user.field.max-length.message}")
    private String firstName;

    @Size(max = 25, message = "{openl.constraints.user.field.max-length.message}")
    private String lastName;

    public String getEmail() {
        return email;
    }

    public UserInfoModel setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UserInfoModel setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public UserInfoModel setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public UserInfoModel setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }
}
