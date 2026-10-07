package org.openl.studio.projects.model.history;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.Parameter;
import org.jspecify.annotations.NonNull;

public record RestoreProjectHistoryRequest(
        @Parameter(description = "Local history entry identifier to restore.")
        @NotBlank
        @NonNull String version
) {
}
