package org.openl.rules.rest.model;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;

import org.openl.rules.rest.validation.ChangePasswordConstraint;

public class UserProfileEditModel extends UserProfileBaseModel {

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
    @NotBlank
    public String getEmail() {
        return super.getEmail();
    }

    @Override
    public UserProfileEditModel setEmail(String email) {
        return (UserProfileEditModel) super.setEmail(email);
    }

    @Override
    @NotBlank
    public String getDisplayName() {
        return super.getDisplayName();
    }

    @Override
    public UserProfileEditModel setDisplayName(String displayName) {
        return (UserProfileEditModel) super.setDisplayName(displayName);
    }

    @Override
    public UserProfileEditModel setShowHeader(boolean showHeader) {
        return (UserProfileEditModel) super.setShowHeader(showHeader);
    }

    @Override
    public UserProfileEditModel setShowFormulas(boolean showFormulas) {
        return (UserProfileEditModel) super.setShowFormulas(showFormulas);
    }

    @Override
    public UserProfileEditModel setTestsPerPage(int testsPerPage) {
        return (UserProfileEditModel) super.setTestsPerPage(testsPerPage);
    }

    @Override
    public UserProfileEditModel setTestsFailuresOnly(boolean testsFailuresOnly) {
        return (UserProfileEditModel) super.setTestsFailuresOnly(testsFailuresOnly);
    }

    @Override
    public UserProfileEditModel setTestsFailuresPerTest(int testsFailuresPerTest) {
        return (UserProfileEditModel) super.setTestsFailuresPerTest(testsFailuresPerTest);
    }

    @Override
    public UserProfileEditModel setShowComplexResult(boolean showComplexResult) {
        return (UserProfileEditModel) super.setShowComplexResult(showComplexResult);
    }

    @Override
    public UserProfileEditModel setShowRealNumbers(boolean showRealNumbers) {
        return (UserProfileEditModel) super.setShowRealNumbers(showRealNumbers);
    }

    @Override
    public UserProfileEditModel setTreeView(String defaultOrder) {
        return (UserProfileEditModel) super.setTreeView(defaultOrder);
    }
}
