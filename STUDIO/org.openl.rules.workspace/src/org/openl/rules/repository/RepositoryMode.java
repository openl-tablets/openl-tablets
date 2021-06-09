package org.openl.rules.repository;

public enum RepositoryMode {
    DESIGN,
    DEPLOY_CONFIG,
    PRODUCTION;

    //FIXME remove after implementation of unification of default settings
    public String getId() {
        return name().toLowerCase().replaceAll("_", "-");
    }
}
