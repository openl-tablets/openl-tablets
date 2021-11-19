package org.openl.security.saml;

import org.springframework.core.env.PropertyResolver;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.DefaultSaml2AuthenticationRequestContextResolver;
import org.springframework.security.saml2.provider.service.web.RelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationRequestContextResolver;

/**
 * Used for configuration security-saml.xml.
 *
 * @author Eugene Biruk
 */
public class OpenLSamlBuilder {

    private LazyInMemoryRelyingPartyRegistrationRepository relyingPartyRegistrationRepository;

    public OpenLSamlBuilder(PropertyResolver propertyResolver) {
        relyingPartyRegistrationRepository = new LazyInMemoryRelyingPartyRegistrationRepository(propertyResolver);
    }

    /**
     * Create RelyingPartyRegistrationRepository instance of LazyInMemoryRelyingPartyRegistrationRepository.
     * @return RelyingPartyRegistrationRepository
     */
    public RelyingPartyRegistrationRepository relyingPartyRegistration() {
        return relyingPartyRegistrationRepository;
    }

    /**
     * Create DefaultSaml2AuthenticationRequestContextResolver based on DefaultRelyingPartyRegistrationResolver.
     * @return DefaultSaml2AuthenticationRequestContextResolver
     */
    public Saml2AuthenticationRequestContextResolver authenticationRequestContextResolver() {
        return new DefaultSaml2AuthenticationRequestContextResolver(relyingPartyRegistrationResolver());
    }

    /**
     * Create DefaultRelyingPartyRegistrationResolver based on LazyInMemoryRelyingPartyRegistrationRepository.
     * @return DefaultRelyingPartyRegistrationResolver
     */
    public RelyingPartyRegistrationResolver relyingPartyRegistrationResolver() {
        return new DefaultRelyingPartyRegistrationResolver(relyingPartyRegistrationRepository);
    }

}
