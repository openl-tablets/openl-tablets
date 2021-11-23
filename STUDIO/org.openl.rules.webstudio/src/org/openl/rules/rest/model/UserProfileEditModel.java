package org.openl.rules.rest.model;

import org.openl.rules.rest.validation.ChangePasswordConstraint;

public class UserProfileEditModel extends UserProfileBaseModel {

    @ChangePasswordConstraint
    private ChangePasswordModel changePassword;

    public ChangePasswordModel getChangePassword() {
        return changePassword;
    }

    public UserProfileEditModel setChangePassword(ChangePasswordModel changePassword) {
        this.changePassword = changePassword;
        return this;
    }

    @Override
    public String getFirstName() {
        return super.getFirstName();
    }

    @Override
    public UserProfileEditModel setFirstName(String firstName) {
        return (UserProfileEditModel) super.setFirstName(firstName);
    }

    @Override
    public String getLastName() {
        return super.getLastName();
    }

    @Override
    public UserProfileEditModel setLastName(String lastName) {
        return (UserProfileEditModel) super.setLastName(lastName);
    }

    @Override
    public String getEmail() {
        return super.getEmail();
    }

    @Override
    public UserProfileEditModel setEmail(String email) {
        return (UserProfileEditModel) super.setEmail(email);
    }

    @Override
    public String getDisplayName() {
        return super.getDisplayName();
    }

    @Override
    public UserProfileEditModel setDisplayName(String displayName) {
        return (UserProfileEditModel) super.setDisplayName(displayName);
    }

    @Override
    public boolean isShowHeader() {
        return super.isShowHeader();
    }

    @Override
    public UserProfileEditModel setShowHeader(boolean showHeader) {
        return (UserProfileEditModel) super.setShowHeader(showHeader);
    }

    @Override
    public boolean isShowFormulas() {
        return super.isShowFormulas();
    }

    @Override
    public UserProfileEditModel setShowFormulas(boolean showFormulas) {
        return (UserProfileEditModel) super.setShowFormulas(showFormulas);
    }

    @Override
    public int getTestsPerPage() {
        return super.getTestsPerPage();
    }

    @Override
    public UserProfileEditModel setTestsPerPage(int testsPerPage) {
        return (UserProfileEditModel) super.setTestsPerPage(testsPerPage);
    }

    @Override
    public boolean isTestsFailuresOnly() {
        return super.isTestsFailuresOnly();
    }

    @Override
    public UserProfileEditModel setTestsFailuresOnly(boolean testsFailuresOnly) {
        return (UserProfileEditModel) super.setTestsFailuresOnly(testsFailuresOnly);
    }

    @Override
    public int getTestsFailuresPerTest() {
        return super.getTestsFailuresPerTest();
    }

    @Override
    public UserProfileEditModel setTestsFailuresPerTest(int testsFailuresPerTest) {
        return (UserProfileEditModel) super.setTestsFailuresPerTest(testsFailuresPerTest);
    }

    @Override
    public boolean isShowComplexResult() {
        return super.isShowComplexResult();
    }

    @Override
    public UserProfileEditModel setShowComplexResult(boolean showComplexResult) {
        return (UserProfileEditModel) super.setShowComplexResult(showComplexResult);
    }

    @Override
    public boolean isShowRealNumbers() {
        return super.isShowRealNumbers();
    }

    @Override
    public UserProfileEditModel setShowRealNumbers(boolean showRealNumbers) {
        return (UserProfileEditModel) super.setShowRealNumbers(showRealNumbers);
    }
}
