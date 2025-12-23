package org.openl.studio.users.service.pat;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import org.openl.rules.security.standalone.dao.PersonalAccessTokenDao;
import org.openl.rules.security.standalone.persistence.PersonalAccessToken;
import org.openl.studio.security.pat.model.PatToken;
import org.openl.studio.users.model.pat.PersonalAccessTokenResponse;

/**
 * Default implementation of {@link PersonalAccessTokenService}.
 * <p>
 * This service provides database operations for managing Personal Access Tokens,
 * acting as a bridge between the REST API layer and the DAO layer. All methods
 * are transactional and exclude sensitive token secret hashes from responses.
 * </p>
 * <p>
 * Security considerations:
 * <ul>
 *   <li>Token secrets are never returned by any method</li>
 *   <li>Public ID validation prevents injection attacks</li>
 *   <li>User ownership is verified before returning token details</li>
 * </ul>
 * </p>
 *
 * @since 6.0.0
 */
public class PersonalAccessTokenServiceImpl implements PersonalAccessTokenService {

    private final PersonalAccessTokenDao tokenDao;

    /**
     * Constructs a new PersonalAccessTokenServiceImpl.
     *
     * @param tokenDao the DAO for accessing Personal Access Token entities (must not be null)
     */
    public PersonalAccessTokenServiceImpl(PersonalAccessTokenDao tokenDao) {
        this.tokenDao = tokenDao;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation queries the database for all tokens belonging to the user
     * and converts them to response objects excluding secret hashes.
     * </p>
     */
    @Transactional(readOnly = true)
    @Override
    public List<PersonalAccessTokenResponse> getTokensByUser(String loginName) {
        return tokenDao.getByLoginName(loginName).stream().map(this::toTokenResponse).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation validates the public ID format (Base62 alphanumeric, exactly 16 chars)
     * and verifies that the token belongs to the specified user before returning it.
     * </p>
     */
    @Transactional(readOnly = true)
    @Override
    public PersonalAccessTokenResponse getTokenForUser(String publicId, String loginName) {
        // Use constants from PatToken to ensure consistency
        if (!PatToken.isValidPublicId(publicId)) {
            return null;
        }
        PersonalAccessToken token = tokenDao.getByPublicId(publicId);
        if (token == null || !loginName.equals(token.getLoginName())) {
            return null;
        }

        return toTokenResponse(token);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation performs a database lookup to check for name uniqueness.
     * </p>
     */
    @Transactional(readOnly = true)
    @Override
    public boolean existsByLoginNameAndName(String loginName, String name) {
        return tokenDao.getByLoginNameAndName(loginName, name) != null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation delegates to the DAO to persist the token entity.
     * </p>
     */
    @Transactional
    @Override
    public void save(PersonalAccessToken token) {
        tokenDao.save(token);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation delegates to the DAO to remove the token from the database.
     * </p>
     */
    @Transactional
    @Override
    public void deleteByPublicId(String publicId) {
        tokenDao.deleteByPublicId(publicId);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation delegates to the DAO to remove all user tokens in a single operation.
     * </p>
     */
    @Transactional
    @Override
    public void deleteAllByUser(String loginName) {
        tokenDao.deleteAllByLoginName(loginName);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation delegates to the DAO for an efficient existence check.
     * </p>
     */
    @Transactional(readOnly = true)
    @Override
    public boolean existsByPublicId(String publicId) {
        return tokenDao.existsByPublicId(publicId);
    }

    /**
     * Converts a PersonalAccessToken entity to a response DTO.
     * <p>
     * This method excludes the secret hash for security reasons.
     * Only public token metadata is included in the response.
     * </p>
     *
     * @param token the token entity to convert
     * @return the response DTO with public token information
     */
    private PersonalAccessTokenResponse toTokenResponse(PersonalAccessToken token) {
        return PersonalAccessTokenResponse.builder()
                .publicId(token.getPublicId())
                .name(token.getName())
                .loginName(token.getLoginName())
                .createdAt(token.getCreatedAt())
                .expiresAt(token.getExpiresAt())
                .build();
    }
}
