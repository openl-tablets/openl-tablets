package org.openl.rules.rest.model;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;

import org.openl.rules.security.UserExternalFlags;
import org.openl.rules.ui.tree.view.RulesProfile;

public class UserProfileModel extends UserProfileBaseModel {

    @Getter
    @Parameter(description = "Username")
    private String username;

    @Getter
    private UserExternalFlags externalFlags;

    @Getter
    private RulesProfile[] profiles;

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
    public UserProfileModel setShowHeader(boolean showHeader) {
        return (UserProfileModel) super.setShowHeader(showHeader);
    }

    @Override
    public UserProfileModel setShowFormulas(boolean showFormulas) {
        return (UserProfileModel) super.setShowFormulas(showFormulas);
    }

    @Override
    public UserProfileModel setTestsPerPage(int testsPerPage) {
        return (UserProfileModel) super.setTestsPerPage(testsPerPage);
    }

    @Override
    public UserProfileModel setTestsFailuresOnly(boolean testsFailuresOnly) {
        return (UserProfileModel) super.setTestsFailuresOnly(testsFailuresOnly);
    }

    @Override
    public UserProfileModel setTestsFailuresPerTest(int testsFailuresPerTest) {
        return (UserProfileModel) super.setTestsFailuresPerTest(testsFailuresPerTest);
    }

    @Override
    public UserProfileModel setShowComplexResult(boolean showComplexResult) {
        return (UserProfileModel) super.setShowComplexResult(showComplexResult);
    }

    @Override
    public UserProfileModel setShowRealNumbers(boolean showRealNumbers) {
        return (UserProfileModel) super.setShowRealNumbers(showRealNumbers);
    }

    @Override
    public UserProfileModel setTreeView(String treeView) {
        return (UserProfileModel) super.setTreeView(treeView);
    }

    public UserProfileModel setProfiles(RulesProfile[] profiles) {
        this.profiles = profiles;
        return this;
    }

    public UserProfileModel setAdministrator(boolean administrator) {
        this.administrator = administrator;
        return this;
    }
}
