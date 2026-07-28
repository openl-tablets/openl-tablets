package org.openl.rules.rest.model;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;

public class ChangePasswordModel {

    @Getter
    @Parameter(description = "New password")
    private String newPassword;

    @Getter
    @Parameter(description = "Confirm password")
    private String confirmPassword;

    @Getter
    @Parameter(description = "Current password")
    private String currentPassword;

    public ChangePasswordModel setNewPassword(String newPassword) {
        this.newPassword = newPassword;
        return this;
    }

    public ChangePasswordModel setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
        return this;
    }

    public ChangePasswordModel setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
        return this;
    }
}
