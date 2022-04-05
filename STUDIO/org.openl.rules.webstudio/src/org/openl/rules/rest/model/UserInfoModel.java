package org.openl.rules.rest.model;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.Email;
import javax.validation.constraints.Size;

public class UserInfoModel {

    @Email(message = "{openl.constraints.user.email.format.message}")
    @Size(max = 254, message = "{openl.constraints.size.max.message}")
    @Schema(description = "User e-mail", example = "test@test")
    private String email;

    @Size(max = 64, message = "{openl.constraints.size.max.message}")
    @Schema(description = "User display name", example = "John Doe")
    private String displayName;

    @Size(max = 25, message = "{openl.constraints.size.max.message}")
    @Schema(description = "User first name", example = "John")
    private String firstName;

    @Size(max = 25, message = "{openl.constraints.size.max.message}")
    @Schema(description = "User last name", example = "Doe")
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
