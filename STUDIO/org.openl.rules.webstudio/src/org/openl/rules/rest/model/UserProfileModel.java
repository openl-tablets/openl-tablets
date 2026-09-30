package org.openl.rules.rest.model;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import org.openl.rules.security.UserExternalFlags;

public class UserProfileModel extends UserProfileBaseModel {

    @Getter
    @Parameter(description = "Username")
    private String username;

    @Getter
    private UserExternalFlags externalFlags;

    @Getter
    private boolean administrator;

    public UserProfileModel setUsername(String username) {
        this.username = username;
        return this;
    }

    public UserProfileModel setExternalFlags(UserExternalFlags externalFlags) {
        this.externalFlags = externalFlags;
        return this;
    }

    @Override
    public UserProfileModel setFirstName(String firstName) {
        return (UserProfileModel) super.setFirstName(firstName);
    }

    @Override
    public UserProfileModel setLastName(String lastName) {
        return (UserProfileModel) super.setLastName(lastName);
    }

    @Override
    public UserProfileModel setEmail(String email) {
        return (UserProfileModel) super.setEmail(email);
    }

    @Override
    public UserProfileModel setDisplayName(String displayName) {
        return (UserProfileModel) super.setDisplayName(displayName);
    }

    @Override
    public UserProfileModel setShowHeader(@Nullable Boolean showHeader) {
        return (UserProfileModel) super.setShowHeader(showHeader);
    }

    @Override
    public UserProfileModel setShowFormulas(@Nullable Boolean showFormulas) {
        return (UserProfileModel) super.setShowFormulas(showFormulas);
    }

    @Override
    public UserProfileModel setTestsPerPage(@Nullable Integer testsPerPage) {
        return (UserProfileModel) super.setTestsPerPage(testsPerPage);
    }

    @Override
    public UserProfileModel setTestsFailuresOnly(@Nullable Boolean testsFailuresOnly) {
        return (UserProfileModel) super.setTestsFailuresOnly(testsFailuresOnly);
    }

    @Override
    public UserProfileModel setTestsFailuresPerTest(@Nullable Integer testsFailuresPerTest) {
        return (UserProfileModel) super.setTestsFailuresPerTest(testsFailuresPerTest);
    }

    @Override
    public UserProfileModel setShowComplexResult(@Nullable Boolean showComplexResult) {
        return (UserProfileModel) super.setShowComplexResult(showComplexResult);
    }

    @Override
    public UserProfileModel setShowRealNumbers(@Nullable Boolean showRealNumbers) {
        return (UserProfileModel) super.setShowRealNumbers(showRealNumbers);
    }

    public UserProfileModel setAdministrator(boolean administrator) {
        this.administrator = administrator;
        return this;
    }
}
