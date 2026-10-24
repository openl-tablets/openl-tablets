package org.openl.rules.rest.model;

import jakarta.validation.constraints.Pattern;

import lombok.Getter;

import org.openl.rules.rest.validation.ChangePasswordConstraint;

/**
 * A change to the profile of the current user.
 *
 * <p>A field that the request leaves out, or sends as {@code null}, keeps the value stored for the user, such as the
 * e-mail or a name.
 *
 * <p>An e-mail or a display name that the request sends must not be blank.
 */
public class UserProfileEditModel extends UserInfoModel {

    /** A value holding more than white space. */
    private static final String NOT_BLANK = "(?s).*\\S.*";

    @ChangePasswordConstraint
    @Getter
    private ChangePasswordModel changePassword;

    public UserProfileEditModel setChangePassword(ChangePasswordModel changePassword) {
        this.changePassword = changePassword;
        return this;
    }

    @Override
    public UserProfileEditModel setFirstName(String firstName) {
        return (UserProfileEditModel) super.setFirstName(firstName);
    }

    @Override
    public UserProfileEditModel setLastName(String lastName) {
        return (UserProfileEditModel) super.setLastName(lastName);
    }

    @Override
    @Pattern(regexp = NOT_BLANK, message = "{jakarta.validation.constraints.NotBlank.message}")
    public String getEmail() {
        return super.getEmail();
    }

    @Override
    public UserProfileEditModel setEmail(String email) {
        return (UserProfileEditModel) super.setEmail(email);
    }

    @Override
    @Pattern(regexp = NOT_BLANK, message = "{jakarta.validation.constraints.NotBlank.message}")
    public String getDisplayName() {
        return super.getDisplayName();
    }

    @Override
    public UserProfileEditModel setDisplayName(String displayName) {
        return (UserProfileEditModel) super.setDisplayName(displayName);
    }
}
