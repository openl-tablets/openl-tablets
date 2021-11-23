package org.openl.rules.security;

public class UserExternalFlags {
    private final boolean firstNameExternal;
    private final boolean lastNameExternal;
    private final boolean emailExternal;
    private final boolean displayNameExternal;

    public UserExternalFlags(boolean firstNameExternal,
            boolean lastNameExternal,
            boolean emailExternal,
            boolean displayNameExternal) {
        this.firstNameExternal = firstNameExternal;
        this.lastNameExternal = lastNameExternal;
        this.emailExternal = emailExternal;
        this.displayNameExternal = displayNameExternal;
    }

    public UserExternalFlags() {
        this.firstNameExternal = false;
        this.lastNameExternal = false;
        this.emailExternal = false;
        this.displayNameExternal = false;
    }

    public boolean isFirstNameExternal() {
        return firstNameExternal;
    }

    public boolean isLastNameExternal() {
        return lastNameExternal;
    }

    public boolean isEmailExternal() {
        return emailExternal;
    }

    public boolean isDisplayNameExternal() {
        return displayNameExternal;
    }
}
