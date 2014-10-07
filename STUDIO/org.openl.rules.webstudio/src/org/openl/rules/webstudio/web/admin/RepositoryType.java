package org.openl.rules.webstudio.web.admin;

public enum RepositoryType {
    DESIGN,
    PRODUCTION;

    @Override public String toString() {
        return name().toLowerCase();
    }

}
