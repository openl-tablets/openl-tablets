package org.openl.security.acl.repository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum AclRepositoryType {

    DESIGN("design"),
    PROD("prod");

    private final String type;

    public String getType() {
        return type;
    }
}
