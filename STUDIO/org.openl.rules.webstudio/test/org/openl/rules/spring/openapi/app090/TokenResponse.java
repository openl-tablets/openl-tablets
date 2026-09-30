package org.openl.rules.spring.openapi.app090;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response model for token operations.
 */
@Schema(description = "Token information")
public record TokenResponse(
        @Schema(description = "Token ID", example = "token-123")
        String id,

        @Schema(description = "Token name", example = "My API Token")
        String name
) {
}
