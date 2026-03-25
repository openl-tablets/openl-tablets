package org.openl.studio.users.model.pat;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Response model for Personal Access Token (without secret).
 * Used for listing tokens.
 */
@Builder
public record PersonalAccessTokenResponse(
        @Schema(description = "pat.field.public-id.desc", example = "a1b2c3d4e5f6")
        String publicId,

        @Schema(description = "pat.field.name.desc", example = "MCP Client Token")
        String name,

        @Schema(description = "pat.field.login-name.desc", example = "john.doe")
        String loginName,

        @Schema(description = "pat.field.created-at.desc")
        Instant createdAt,

        @Schema(description = "pat.field.expires-at.desc")
        Instant expiresAt) {
}
