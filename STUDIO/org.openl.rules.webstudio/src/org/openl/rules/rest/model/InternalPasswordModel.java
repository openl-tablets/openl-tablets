package org.openl.rules.rest.model;

public class InternalPasswordModel {

    private String password;

    private boolean internalUser;

    public String getPassword() {
        return password;
    }

    public InternalPasswordModel setPassword(String password) {
        this.password = password;
        return this;
    }

    public boolean isInternalUser() {
        return internalUser;
    }

    public InternalPasswordModel setInternalUser(boolean internalUser) {
        this.internalUser = internalUser;
        return this;
    }
}
