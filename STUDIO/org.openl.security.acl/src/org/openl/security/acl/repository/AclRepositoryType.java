package org.openl.security.acl.repository;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum AclRepositoryType {

    DESIGN("design"),
    PROD("prod");

    @Getter
    private final String type;
}
