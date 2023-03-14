package org.openl.rules.rest.model;

import io.swagger.v3.oas.annotations.Parameter;

public class RepositoryFeatures {

    @Parameter(description = "Determines if the target repository supports branching")
    private final boolean branches;
    @Parameter(description = "Determines if the target repository supports searching and pagination")
    private final boolean searchable;

    public RepositoryFeatures(boolean branches, boolean searchable) {
        this.branches = branches;
        this.searchable = searchable;
    }

    public boolean isBranches() {
        return branches;
    }

    public boolean isSearchable() {
        return searchable;
    }
}
