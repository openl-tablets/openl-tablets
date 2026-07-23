package org.openl.rules.rest.model;

import java.time.Instant;
import java.util.Set;

import io.swagger.v3.oas.annotations.Parameter;

import org.openl.rules.security.UserExternalFlags;

public class UserModel extends UserCreateModel {

    @Parameter(description = "Current user marker")
    private boolean currentUser;

    @Parameter(description = "Superuser marker")
    private boolean superUser;

    @Parameter(description = "Unsafe password marker")
    private boolean unsafePassword;
    private UserExternalFlags externalFlags;

    @Parameter(description = "Number of external groups which is unknown by OpenL Studio")
    private Long notMatchedExternalGroupsCount;

    @Parameter(description = "User online marker")
    private boolean online;

    @Parameter(description = "Time of the last successful sign-in")
    private Instant lastLoginTime;

    private Set<GroupModel> userGroups;

    public Set<GroupModel> getUserGroups() {
        return userGroups;
    }

    public UserModel setUserGroups(Set<GroupModel> userGroups) {
        this.userGroups = userGroups;
        return this;
    }

    @Override
    public UserModel setInternalPassword(InternalPasswordModel internalPassword) {
        return (UserModel) super.setInternalPassword(internalPassword);
    }

    @Override
    public UserModel setUsername(String username) {
        return (UserModel) super.setUsername(username);
    }

    @Override
    public UserModel setPassword(String password) {
        return (UserModel) super.setPassword(password);
    }

    public boolean isCurrentUser() {
        return currentUser;
    }

    public UserModel setCurrentUser(boolean currentUser) {
        this.currentUser = currentUser;
        return this;
    }

    public boolean isSuperUser() {
        return superUser;
    }

    public UserModel setSuperUser(boolean superUser) {
        this.superUser = superUser;
        return this;
    }

    public boolean isUnsafePassword() {
        return unsafePassword;
    }

    public UserModel setUnsafePassword(boolean unsafePassword) {
        this.unsafePassword = unsafePassword;
        return this;
    }

    public UserExternalFlags getExternalFlags() {
        return externalFlags;
    }

    public UserModel setExternalFlags(UserExternalFlags externalFlags) {
        this.externalFlags = externalFlags;
        return this;
    }

    @Override
    public UserModel setFirstName(String firstName) {
        return (UserModel) super.setFirstName(firstName);
    }

    @Override
    public UserModel setLastName(String lastName) {
        return (UserModel) super.setLastName(lastName);
    }

    @Override
    public UserModel setEmail(String email) {
        return (UserModel) super.setEmail(email);
    }

    @Override
    public UserModel setDisplayName(String displayName) {
        return (UserModel) super.setDisplayName(displayName);
    }

    @Override
    public UserModel setGroups(Set<String> groups) {
        return (UserModel) super.setGroups(groups);
    }

    public Long getNotMatchedExternalGroupsCount() {
        return notMatchedExternalGroupsCount;
    }

    public UserModel setNotMatchedExternalGroupsCount(Long notMatchedExternalGroupsCount) {
        this.notMatchedExternalGroupsCount = notMatchedExternalGroupsCount;
        return this;
    }

    public boolean isOnline() {
        return online;
    }

    public UserModel setOnline(boolean online) {
        this.online = online;
        return this;
    }

    public Instant getLastLoginTime() {
        return lastLoginTime;
    }

    public UserModel setLastLoginTime(Instant lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
        return this;
    }
}
