package org.openl.studio.projects.model.history;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.Parameter;
import org.jspecify.annotations.NonNull;

public record CompareProjectHistoryRequest(
        @Parameter(description = "Local history entry identifier shown on the first side.")
        @NotBlank
        @NonNull String first,

        @Parameter(description = "Local history entry identifier shown on the second side.")
        @NotBlank
        @NonNull String second
) {
}
