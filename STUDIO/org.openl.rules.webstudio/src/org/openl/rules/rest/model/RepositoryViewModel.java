package org.openl.rules.rest.model;

import io.swagger.v3.oas.annotations.media.Schema;

public class RepositoryViewModel {

    @Schema(description = "Repository unique identifier. Used as identifier in all requests", required = true)
    private final String id;

    @Schema(description = "Repository display name", required = true)
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
