package org.openl.rules.rest.model;

import jakarta.validation.constraints.Pattern;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import org.openl.rules.rest.validation.ChangePasswordConstraint;
import org.openl.rules.rest.validation.TestsCountConstraint;

/**
 * A change to the profile of the current user.
 *
 * <p>A field that the request leaves out, or sends as {@code null}, keeps the value stored for the user: a detail such
 * as the e-mail or a name, and a setting alike.
 *
 * <p>An e-mail or a display name that the request sends must not be blank.
 */
public class UserProfileEditModel extends UserProfileBaseModel {

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

    @Override
    public UserProfileEditModel setShowHeader(@Nullable Boolean showHeader) {
        return (UserProfileEditModel) super.setShowHeader(showHeader);
    }

    @Override
    public UserProfileEditModel setShowFormulas(@Nullable Boolean showFormulas) {
        return (UserProfileEditModel) super.setShowFormulas(showFormulas);
    }

    @Override
    public UserProfileEditModel setShowExcelFormatting(@Nullable Boolean showExcelFormatting) {
        return (UserProfileEditModel) super.setShowExcelFormatting(showExcelFormatting);
    }

    @Override
    @TestsCountConstraint
    public @Nullable Integer getTestsPerPage() {
        return super.getTestsPerPage();
    }

    @Override
    public UserProfileEditModel setTestsPerPage(@Nullable Integer testsPerPage) {
        return (UserProfileEditModel) super.setTestsPerPage(testsPerPage);
    }

    @Override
    public UserProfileEditModel setTestsFailuresOnly(@Nullable Boolean testsFailuresOnly) {
        return (UserProfileEditModel) super.setTestsFailuresOnly(testsFailuresOnly);
    }

    @Override
    @TestsCountConstraint
    public @Nullable Integer getTestsFailuresPerTest() {
        return super.getTestsFailuresPerTest();
    }

    @Override
    public UserProfileEditModel setTestsFailuresPerTest(@Nullable Integer testsFailuresPerTest) {
        return (UserProfileEditModel) super.setTestsFailuresPerTest(testsFailuresPerTest);
    }

    @Override
    public UserProfileEditModel setShowComplexResult(@Nullable Boolean showComplexResult) {
        return (UserProfileEditModel) super.setShowComplexResult(showComplexResult);
    }

    @Override
    public UserProfileEditModel setShowRealNumbers(@Nullable Boolean showRealNumbers) {
        return (UserProfileEditModel) super.setShowRealNumbers(showRealNumbers);
    }
}
