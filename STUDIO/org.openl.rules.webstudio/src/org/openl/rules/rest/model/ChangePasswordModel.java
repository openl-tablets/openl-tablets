package org.openl.rules.rest.model;

public class ChangePasswordModel {

    private String newPassword;

    private String confirmPassword;

    private String currentPassword;

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
