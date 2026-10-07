package org.openl.studio.users.service.pat;

import java.util.List;

import org.openl.rules.security.standalone.persistence.PersonalAccessToken;
import org.openl.studio.users.model.pat.PersonalAccessTokenResponse;

/**
 * CRUD service for managing Personal Access Tokens.
 * <p>
 * This service provides database operations for Personal Access Token entities,
 * including creation, retrieval, and deletion. It acts as a data access layer
 * between the REST API and the database DAO.
 * </p>
 * <p>
 * All methods that return token information exclude the secret hash for security.
 * Token secrets are only available once during generation via {@link org.openl.studio.security.pat.service.PatGeneratorService}.
 * </p>
 *
 * @since 6.0.0
 */
public interface PersonalAccessTokenService {

    /**
     * Retrieves all tokens for a specific user.
     * <p>
     * Returns a list of all tokens owned by the user, excluding secret hashes.
     * Tokens are returned in no particular order.
     * </p>
     *
     * @param loginName the user's login name (must not be null)
     * @return list of token responses (never null, may be empty)
     */
    List<PersonalAccessTokenResponse> getTokensByUser(String loginName);

    /**
     * Retrieves a specific token for a user by its public ID.
     * <p>
     * This method verifies that the token with the given public ID belongs to the specified user.
     * Returns null if the token doesn't exist or doesn't belong to the user.
     * </p>
     *
     * @param publicId  the token's public identifier (must not be null)
     * @param loginName the user's login name (must not be null)
     * @return token response if found and owned by user, null otherwise
     */
    PersonalAccessTokenResponse getTokenForUser(String publicId, String loginName);

    /**
     * Checks if a token with the given name already exists for a user.
     * <p>
     * This method is used to prevent duplicate token names for the same user
     * during token creation. Token names must be unique per user.
     * </p>
     *
     * @param loginName the user's login name (must not be null)
     * @param name      the token name to check (must not be null)
     * @return true if a token with this name exists for the user, false otherwise
     */
    boolean existsByLoginNameAndName(String loginName, String name);

    /**
     * Saves a Personal Access Token to the database.
     * <p>
     * This method persists a new token or updates an existing one.
     * The token's secret should be hashed before calling this method.
     * </p>
     *
     * @param token the token entity to save (must not be null, secret must be hashed)
     */
    void save(PersonalAccessToken token);

    /**
     * Deletes a token by its public ID.
     * <p>
     * Removes the token from the database permanently. This operation cannot be undone.
     * If no token with the given public ID exists, this method has no effect.
     * </p>
     *
     * @param publicId the token's public identifier (must not be null)
     */
    void deleteByPublicId(String publicId);

    /**
     * Deletes all tokens belonging to a specific user.
     * <p>
     * This method is typically used when a user account is deleted or when
     * all user tokens need to be revoked at once. The operation is permanent
     * and cannot be undone.
     * </p>
     *
     * @param loginName the user's login name (must not be null)
     */
    void deleteAllByUser(String loginName);

    /**
     * Checks if a token exists with the given public ID.
     * <p>
     * This method performs a lightweight existence check without loading
     * the full token entity from the database.
     * </p>
     *
     * @param publicId the token's public identifier (must not be null)
     * @return true if a token with this public ID exists, false otherwise
     */
    boolean existsByPublicId(String publicId);

}
