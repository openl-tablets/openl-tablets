package org.openl.studio.users.model.pat;

import java.time.Instant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request model for creating a new Personal Access Token.
 */
public record CreatePersonalAccessTokenRequest(
        @Schema(description = "pat.field.name.desc", example = "MCP Client Token")
        @NotBlank(message = "Token name is required")
        @Size(max = 100, message = "Token name must not exceed 100 characters")
        String name,

        @Schema(description = "pat.field.expires-at.desc")
        Instant expiresAt) {
}
