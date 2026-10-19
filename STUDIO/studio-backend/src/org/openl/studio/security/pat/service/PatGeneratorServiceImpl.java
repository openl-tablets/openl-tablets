package org.openl.studio.security.pat.service;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Optional;
import jakarta.validation.constraints.NotBlank;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import org.openl.rules.security.standalone.persistence.PersonalAccessToken;
import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditEvent;
import org.openl.studio.audit.model.AuditTargetType;
import org.openl.studio.security.pat.Base62Generator;
import org.openl.studio.security.pat.model.PatToken;
import org.openl.studio.users.model.pat.CreatedPersonalAccessTokenResponse;
import org.openl.studio.users.service.pat.PersonalAccessTokenService;

/**
 * Default implementation of {@link PatGeneratorService}.
 * <p>
 * This service generates cryptographically secure Personal Access Tokens with:
 * <ul>
 *   <li>16-character Base62 public ID</li>
 *   <li>32-character Base62 secret</li>
 *   <li>Hashed secret storage for security</li>
 * </ul>
 * </p>
 *
 * @since 6.0.0
 */
@RequiredArgsConstructor
@Validated
public class PatGeneratorServiceImpl implements PatGeneratorService {

    private final PersonalAccessTokenService crudService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * {@inheritDoc}
     * <p>
     * This implementation generates a unique 16-character public ID and a 32-character secret,
     * handles collision detection for public IDs, and stores the token with a hashed secret.
     * </p>
     */
    @Transactional
    @Override
    public CreatedPersonalAccessTokenResponse generateToken(@NotBlank String loginName,
                                                            @NotBlank String name,
                                                            Instant expiresAt) {
        Instant now = Instant.now(clock);

        if (expiresAt != null && expiresAt.isBefore(now)) {
            throw new IllegalArgumentException("expiresAt must be in the future");
        }

        // generate unique publicId (very low collision, but handle it)
        String publicId;
        do {
            publicId = Base62Generator.generate(PatToken.PUBLIC_ID_LENGTH);
        } while (crudService.existsByPublicId(publicId));

        // secret (high entropy)
        String secret = Base62Generator.generate(PatToken.SECRET_LENGTH);
        var secretHash = passwordEncoder.encode(secret);

        var token = new PersonalAccessToken();
        token.setPublicId(publicId);
        token.setSecretHash(secretHash);
        token.setLoginName(loginName);
        token.setName(name);
        token.setCreatedAt(now);
        token.setExpiresAt(expiresAt);

        crudService.save(token);
        var details = new LinkedHashMap<String, Object>();
        details.put("name", name);
        details.put("expiresAt", Optional.ofNullable(expiresAt).map(Instant::toString).orElse(null));
        eventPublisher.publishEvent(AuditEvent.builder()
                .action(AuditAction.TOKEN_CREATED)
                .targetType(AuditTargetType.TOKEN)
                .target(publicId)
                .details(details)
                .build());

        var pat = new PatToken(publicId, secret);

        return CreatedPersonalAccessTokenResponse.builder()
                .publicId(publicId)
                .name(name)
                .loginName(loginName)
                .token(pat.asTokenValue())
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();
    }
}
