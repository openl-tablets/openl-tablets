package org.openl.security.acl.repository;

public enum AclRepositoryType {

    DESIGN("design"),
    PROD("prod");

    private final String type;

    AclRepositoryType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
