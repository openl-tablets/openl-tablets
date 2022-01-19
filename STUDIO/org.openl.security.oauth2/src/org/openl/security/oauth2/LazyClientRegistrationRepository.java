package org.openl.security.oauth2;

import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.PropertyResolver;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * Lazy ClientRegistrationRepository initialization, for the case when AS is not available for some reason.
 *
 * @author Eugene Biruk
 */
public class LazyClientRegistrationRepository implements ClientRegistrationRepository {

    private static final Logger log = LoggerFactory.getLogger(LazyClientRegistrationRepository.class);

    private volatile ClientRegistrationRepository clientRegistrationRepository;
    private final PropertyResolver propertyResolver;

    public LazyClientRegistrationRepository(PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
    }

    private void init() {
        try {
            ClientRegistration clientRegistration = ClientRegistrations
                .fromOidcIssuerLocation(propertyResolver.getProperty("security.oauth2.issuer-uri"))
                .clientId(propertyResolver.getProperty("security.oauth2.client-id"))
                .registrationId("webstudio")
                .clientSecret(propertyResolver.getProperty("security.oauth2.client-secret"))
                .scope(StringUtils.split(propertyResolver.getProperty("security.oauth2.scope"), ','))
                .authorizationGrantType(
                    new AuthorizationGrantType(propertyResolver.getProperty("security.oauth2.grant-type")))
                .build();
            clientRegistrationRepository = new InMemoryClientRegistrationRepository(clientRegistration);
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        if (clientRegistrationRepository == null) {
            synchronized (this) {
                if (clientRegistrationRepository == null) {
                    init();
                }
            }
        }
        return clientRegistrationRepository != null ? clientRegistrationRepository.findByRegistrationId(registrationId)
            : null;
    }
}
