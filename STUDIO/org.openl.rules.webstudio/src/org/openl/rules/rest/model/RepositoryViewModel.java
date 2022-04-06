package org.openl.rules.rest.model;

import io.swagger.v3.oas.annotations.Parameter;

public class RepositoryViewModel {

    @Parameter(description = "Repository unique identifier. Used as identifier in all requests", required = true)
    private final String id;

    @Parameter(description = "Repository display name", required = true)
    private final String name;

    public RepositoryViewModel(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
