package org.openl.rules.repository;

public class CommonUserImpl implements CommonUser {

    private String userName;

    public CommonUserImpl(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

}
