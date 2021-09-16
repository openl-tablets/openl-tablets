package org.openl.rules.rest.model;

public class ChangePasswordModel {

    private String username;

    private String newPassword;

    private String confirmPassword;

    private String currentPassword;

    public String getUsername() {
        return username;
    }

    public ChangePasswordModel setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public ChangePasswordModel setNewPassword(String newPassword) {
        this.newPassword = newPassword;
        return this;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public ChangePasswordModel setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
        return this;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public ChangePasswordModel setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
        return this;
    }
}
