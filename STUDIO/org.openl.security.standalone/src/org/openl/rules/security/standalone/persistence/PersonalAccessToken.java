package org.openl.rules.security.standalone.persistence;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity representing a Personal Access Token (PAT) for user authentication.
 * Tokens are identified by base62-encoded public IDs and store secret hashes
 * of the secret portion for secure validation.
 */
@Entity
@Table(name = "OpenL_PAT_Tokens")
public class PersonalAccessToken implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "publicId", length = 16, nullable = false, unique = true)
    private String publicId;

    @Column(name = "secretHash", nullable = false)
    private String secretHash;

    @Column(name = "createdAt", nullable = false)
    private Instant createdAt;

    @Column(name = "expiresAt")
    private Instant expiresAt;

    @Column(name = "loginName", length = 50, nullable = false)
    private String loginName;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    /**
     * Public identifier of the token (base62 encoded, 24 chars max).
     * This is the primary key and is generated in application code before persisting.
     *
     * @return token public ID
     */
    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    /**
     * Secret identifier hash of the token secret.
     * Used for secure validation without storing the actual secret.
     *
     * @return hash of token secret
     */
    public String getSecretHash() {
        return secretHash;
    }

    public void setSecretHash(String secretHash) {
        this.secretHash = secretHash;
    }

    /**
     * Timestamp when the token was created.
     *
     * @return creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Timestamp when the token expires.
     * Nullable to allow tokens without expiration.
     *
     * @return expiration timestamp or null if no expiration
     */
    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    /**
     * Login name of the user who owns this token.
     *
     * @return user's login name
     */
    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    /**
     * Human-readable name/description for the token.
     * Must be unique per user (enforced by database constraint).
     *
     * @return token name
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PersonalAccessToken that = (PersonalAccessToken) o;
        return Objects.equals(publicId, that.publicId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(publicId);
    }

    @Override
    public String toString() {
        return "PersonalAccessToken{publicId=" + publicId + ", name=" + name + '}';
    }
}
