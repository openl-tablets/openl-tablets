package org.openl.rules.rest.model;

import lombok.Getter;

public class InternalPasswordModel {

    @Getter
    private String password;

    public InternalPasswordModel setPassword(String password) {
        this.password = password;
        return this;
    }
}
