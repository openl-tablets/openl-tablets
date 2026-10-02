package org.openl.rules.repository;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum RepositoryMode {

    @JsonProperty("design")
    DESIGN,

    @JsonProperty("production")
    PRODUCTION;

    // The default settings of the repository modes are named by this id until they are unified.
    @SuppressWarnings("java:S1134")
    public String getId() {
        // FIXME remove after implementation of unification of default settings
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
