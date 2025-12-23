package org.openl.studio.users.model.pat;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response model for newly created Personal Access Token.
 * Includes the full token which is only shown once.
 */
public record CreatedPersonalAccessTokenResponse(
        @Schema(description = "pat.field.public-id.desc", example = "a1b2c3d4e5f6")
        String publicId,

        @Schema(description = "pat.field.name.desc", example = "MCP Client Token")
        String name,

        @Schema(description = "pat.field.login-name.desc", example = "john.doe")
        String loginName,

        @Schema(description = "pat.field.token.desc", example = "openl_pat_a1b2c3d4e5f6.7g8h9i0j1k2l3m4n5o6p7q8r9s0t1u2v3w4x5y6z")
        String token,

        @Schema(description = "pat.field.created-at.desc")
        Instant createdAt,

        @Schema(description = "pat.field.expires-at.desc")
        Instant expiresAt) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String publicId;
        private String name;
        private String loginName;
        private String token;
        private Instant createdAt;
        private Instant expiresAt;

        public Builder publicId(String publicId) {
            this.publicId = publicId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder loginName(String loginName) {
            this.loginName = loginName;
            return this;
        }

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public CreatedPersonalAccessTokenResponse build() {
            return new CreatedPersonalAccessTokenResponse(publicId, name, loginName, token, createdAt, expiresAt);
        }
    }
}
