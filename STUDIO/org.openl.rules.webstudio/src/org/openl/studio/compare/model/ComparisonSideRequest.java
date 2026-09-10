package org.openl.studio.compare.model;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.Parameter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * One side of a comparison of a project: which file, and where it is read from.
 *
 * <p>Without a branch and a revision the file is read from the working copy - the project as its own
 * user has it now. A revision names what the repository holds, and a branch says where to look for it.
 */
public record ComparisonSideRequest(

        @Parameter(description = "Path of the file inside the project, e.g. 'rules/Pricing.xlsx'.")
        @NotBlank
        @NonNull String path,

        @Parameter(description = "Branch the revision belongs to. Left out for the working copy.")
        @Nullable String branch,

        @Parameter(description = "Revision to read the file from. Left out for the working copy.")
        @Nullable String revision
) {
}
