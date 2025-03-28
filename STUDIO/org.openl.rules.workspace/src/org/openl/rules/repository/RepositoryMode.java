package org.openl.rules.repository;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum RepositoryMode {

    @JsonProperty("design")
    DESIGN,

    @JsonProperty("deploy-config")
    DEPLOY_CONFIG,

    @JsonProperty("production")
    PRODUCTION;

    //FIXME remove after implementation of unification of default settings
    public String getId() {
        return name().toLowerCase().replaceAll("_", "-");
    }
}
