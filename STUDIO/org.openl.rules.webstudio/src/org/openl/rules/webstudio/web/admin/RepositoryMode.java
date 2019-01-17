package org.openl.rules.webstudio.web.admin;

public enum RepositoryMode {
    DESIGN,
    PRODUCTION;

    @Override public String toString() {
        return name().toLowerCase();
    }

}
