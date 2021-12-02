package org.openl.rules.rest.model;

import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.Email;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class UserInfoModel {

    @Email(message = "{openl.constraints.user.email.format.message}")
    @Size(max = 254, message = "{openl.constraints.size.max.message}")
    private String email;

    @Size(max = 64, message = "{openl.constraints.size.max.message}")
    private String displayName;

    @Size(max = 25, message = "{openl.constraints.size.max.message}")
    private String firstName;

    @Size(max = 25, message = "{openl.constraints.size.max.message}")
    private String lastName;

    private String username;

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

    @JsonIgnore
    public String getUsername() {
        return username;
    }

    public UserInfoModel setUsername(String username) {
        this.username = username;
        return this;
    }
}
