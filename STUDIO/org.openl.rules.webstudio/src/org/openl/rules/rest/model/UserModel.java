package org.openl.rules.rest.model;

import java.util.Set;

import org.openl.rules.security.UserExternalFlags;

public class UserModel extends UserCreateModel {

    private boolean currentUser;

    private boolean superUser;

    private boolean unsafePassword;

    private UserExternalFlags externalFlags;

    private boolean internalUser;

    public boolean isInternalUser() {
        return internalUser;
    }

    public UserModel setInternalUser(boolean internalUser) {
        this.internalUser = internalUser;
        return this;
    }

    @Override
    public InternalPasswordModel getInternalPassword() {
        return super.getInternalPassword();
    }

    @Override
    public UserModel setInternalPassword(InternalPasswordModel internalPassword) {
        return (UserModel) super.setInternalPassword(internalPassword);
    }

    @Override
    public String getUsername() {
        return super.getUsername();
    }

    @Override
    public UserModel setUsername(String username) {
        return (UserModel) super.setUsername(username);
    }

    @Override
    public String getPassword() {
        return super.getPassword();
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
    public String getFirstName() {
        return super.getFirstName();
    }

    @Override
    public UserModel setFirstName(String firstName) {
        return (UserModel) super.setFirstName(firstName);
    }

    @Override
    public String getLastName() {
        return super.getLastName();
    }

    @Override
    public UserModel setLastName(String lastName) {
        return (UserModel) super.setLastName(lastName);
    }

    @Override
    public String getEmail() {
        return super.getEmail();
    }

    @Override
    public UserModel setEmail(String email) {
        return (UserModel) super.setEmail(email);
    }

    @Override
    public String getDisplayName() {
        return super.getDisplayName();
    }

    @Override
    public UserModel setDisplayName(String displayName) {
        return (UserModel) super.setDisplayName(displayName);
    }

    @Override
    public Set<String> getGroups() {
        return super.getGroups();
    }

    @Override
    public UserModel setGroups(Set<String> groups) {
        return (UserModel) super.setGroups(groups);
    }

}
