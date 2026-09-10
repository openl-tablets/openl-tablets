package org.openl.studio.compare.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.Parameter;
import org.jspecify.annotations.NonNull;

/**
 * The two files of a project to compare, each read from the working copy or from a revision.
 */
public record CompareProjectFilesRequest(

        @Parameter(description = "The file shown on the first side.")
        @NotNull
        @Valid
        @NonNull ComparisonSideRequest first,

        @Parameter(description = "The file shown on the second side.")
        @NotNull
        @Valid
        @NonNull ComparisonSideRequest second
) {
}
