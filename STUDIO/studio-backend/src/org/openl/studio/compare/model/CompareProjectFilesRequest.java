package org.openl.studio.compare.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.Parameter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The two files of a project to compare, each read from the working copy or from a revision.
 *
 * <p>With a base, both files are compared with the version they were made from, as the two versions of a conflict
 * are: the first file is your version, the second one their version.
 */
public record CompareProjectFilesRequest(

        @Parameter(description = "The file shown on the first side.")
        @NotNull
        @Valid
        @NonNull ComparisonSideRequest first,

        @Parameter(description = "The file shown on the second side.")
        @NotNull
        @Valid
        @NonNull ComparisonSideRequest second,

        @Parameter(description = "The version both files were made from, which each of them is compared with. Without it, the two files are compared with each other.")
        @Valid
        @Nullable ComparisonSideRequest base
) {
}
