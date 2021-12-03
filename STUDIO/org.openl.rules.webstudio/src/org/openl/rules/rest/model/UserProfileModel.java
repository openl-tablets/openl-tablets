package org.openl.rules.rest.model;

import org.openl.rules.security.UserExternalFlags;

public class UserProfileModel extends UserProfileBaseModel {

    private String username;

    private boolean internalUser;

    private UserExternalFlags externalFlags;

    public String getUsername() {
        return username;
    }

    public UserProfileModel setUsername(String username) {
        this.username = username;
        return this;
    }

    public UserExternalFlags getExternalFlags() {
        return externalFlags;
    }

    public UserProfileModel setExternalFlags(UserExternalFlags externalFlags) {
        this.externalFlags = externalFlags;
        return this;
    }

    @Override
    public String getFirstName() {
        return super.getFirstName();
    }

    @Override
    public UserProfileModel setFirstName(String firstName) {
        return (UserProfileModel) super.setFirstName(firstName);
    }

    @Override
    public String getLastName() {
        return super.getLastName();
    }

    @Override
    public UserProfileModel setLastName(String lastName) {
        return (UserProfileModel) super.setLastName(lastName);
    }

    @Override
    public String getEmail() {
        return super.getEmail();
    }

    @Override
    public UserProfileModel setEmail(String email) {
        return (UserProfileModel) super.setEmail(email);
    }

    @Override
    public String getDisplayName() {
        return super.getDisplayName();
    }

    @Override
    public UserProfileModel setDisplayName(String displayName) {
        return (UserProfileModel) super.setDisplayName(displayName);
    }

    @Override
    public boolean isShowHeader() {
        return super.isShowHeader();
    }

    @Override
    public UserProfileModel setShowHeader(boolean showHeader) {
        return (UserProfileModel) super.setShowHeader(showHeader);
    }

    @Override
    public boolean isShowFormulas() {
        return super.isShowFormulas();
    }

    @Override
    public UserProfileModel setShowFormulas(boolean showFormulas) {
        return (UserProfileModel) super.setShowFormulas(showFormulas);
    }

    @Override
    public int getTestsPerPage() {
        return super.getTestsPerPage();
    }

    @Override
    public UserProfileModel setTestsPerPage(int testsPerPage) {
        return (UserProfileModel) super.setTestsPerPage(testsPerPage);
    }

    @Override
    public boolean isTestsFailuresOnly() {
        return super.isTestsFailuresOnly();
    }

    @Override
    public UserProfileModel setTestsFailuresOnly(boolean testsFailuresOnly) {
        return (UserProfileModel) super.setTestsFailuresOnly(testsFailuresOnly);
    }

    @Override
    public int getTestsFailuresPerTest() {
        return super.getTestsFailuresPerTest();
    }

    @Override
    public UserProfileModel setTestsFailuresPerTest(int testsFailuresPerTest) {
        return (UserProfileModel) super.setTestsFailuresPerTest(testsFailuresPerTest);
    }

    @Override
    public boolean isShowComplexResult() {
        return super.isShowComplexResult();
    }

    @Override
    public UserProfileModel setShowComplexResult(boolean showComplexResult) {
        return (UserProfileModel) super.setShowComplexResult(showComplexResult);
    }

    @Override
    public boolean isShowRealNumbers() {
        return super.isShowRealNumbers();
    }

    @Override
    public UserProfileModel setShowRealNumbers(boolean showRealNumbers) {
        return (UserProfileModel) super.setShowRealNumbers(showRealNumbers);
    }

    public boolean isInternalUser() {
        return internalUser;
    }

    public UserProfileModel setInternalUser(boolean internalUser) {
        this.internalUser = internalUser;
        return this;
    }

}
