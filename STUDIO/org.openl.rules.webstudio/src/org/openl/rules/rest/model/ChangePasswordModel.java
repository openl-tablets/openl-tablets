package org.openl.rules.rest.model;

import io.swagger.v3.oas.annotations.Parameter;

public class ChangePasswordModel {

    @Parameter(description = "New password")
    private String newPassword;

    @Parameter(description = "Confirm password")
    private String confirmPassword;

    @Parameter(description = "Current password")
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
