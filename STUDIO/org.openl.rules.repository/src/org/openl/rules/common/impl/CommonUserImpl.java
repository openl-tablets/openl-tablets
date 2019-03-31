package org.openl.rules.common.impl;

import org.openl.rules.common.CommonUser;

public class CommonUserImpl implements CommonUser {

    private String userName;

    public CommonUserImpl(String userName) {
        this.userName = userName;
    }

    @Override
    public String getUserName() {
        return userName;
    }

}
