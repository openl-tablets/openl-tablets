package org.openl.studio.repositories.model;

import io.swagger.v3.oas.annotations.Parameter;

import org.openl.rules.repository.api.Features;

public record RepositoryFeatures(
        @Parameter(description = "Determines if the target repository supports branching")
        boolean branches,
        @Parameter(description = "Determines if the target repository supports searching and pagination")
        boolean searchable,
        @Parameter(description = "Determines if the target repository supports mapped project folders")
        boolean mappedFolders,
        @Parameter(description = "Determines if the target repository supports change comments")
        boolean comments) {

    public RepositoryFeatures(Features features) {
        this(features.branches(), features.searchable(), features.mappedFolders(), features.comments());
    }
}
