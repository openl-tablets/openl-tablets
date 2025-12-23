package org.openl.rules.security.standalone.dao;

import java.util.List;

import org.openl.rules.security.standalone.persistence.PersonalAccessToken;

/**
 * DAO for Personal Access Token operations.
 */
public interface PersonalAccessTokenDao extends Dao<PersonalAccessToken> {

    /**
     * Retrieves a token by its public ID.
     *
     * @param publicId token public identifier
     * @return token or null if not found
     */
    PersonalAccessToken getByPublicId(String publicId);

    /**
     * Retrieves all tokens for a specific user.
     *
     * @param loginName user's login name
     * @return list of user's tokens
     */
    List<PersonalAccessToken> getByLoginName(String loginName);

    /**
     * Retrieves a specific token by user and name.
     *
     * @param loginName user's login name
     * @param name token name
     * @return token or null if not found
     */
    PersonalAccessToken getByLoginNameAndName(String loginName, String name);

    /**
     * Deletes a token by its public ID.
     *
     * @param publicId token public identifier
     */
    void deleteByPublicId(String publicId);

    /**
     * Deletes all tokens for a specific user.
     *
     * @param loginName user's login name
     */
    void deleteAllByLoginName(String loginName);

    /**
     * Checks if a token exists with the given public ID.
     *
     * @param publicId token public identifier
     * @return true if exists, false otherwise
     */
    boolean existsByPublicId(String publicId);
}
