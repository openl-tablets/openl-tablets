package org.openl.rules.repository;

public enum RepositoryMode {
    DESIGN,
    DEPLOY_CONFIG,
    PRODUCTION;

    @Override public String toString() {
        return name().toLowerCase();
    }

}
