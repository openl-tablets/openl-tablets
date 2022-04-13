package org.openl.security.saml;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.impl.AuthnRequestMarshaller;
import org.springframework.core.env.PropertyResolver;
import org.springframework.security.saml2.core.OpenSamlInitializationService;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.DefaultSaml2AuthenticationRequestContextResolver;
import org.springframework.security.saml2.provider.service.web.RelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationRequestContextResolver;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Used for configuration security-saml.xml.
 *
 * @author Eugene Biruk
 */
public class OpenLSamlBuilder {

    //Field must be static so that the value can be updated when the property file changes.
    //Since the bean will be recreated in this case, but the link should remain the same.
    //RequireInitialize can be called only once, because of this it must be in a static block and call before
    //the initialization of the rest of the beans of the saml, where the initialization method can be called,
    //for example OpenSaml4AuthenticationProvider.
    private static boolean forceAuthN;

    static {
        OpenSamlInitializationService.requireInitialize(factory -> {
            AuthnRequestMarshaller marshaller = new AuthnRequestMarshaller() {

                @Override
                public Element marshall(XMLObject object, Element element) throws MarshallingException {
                    configureAuthnRequest((AuthnRequest) object);
                    return super.marshall(object, element);
                }

                public Element marshall(XMLObject object, Document document) throws MarshallingException {
                    configureAuthnRequest((AuthnRequest) object);
                    return super.marshall(object, document);
                }

                private void configureAuthnRequest(AuthnRequest authnRequest) {

                    authnRequest.setForceAuthn(forceAuthN);
                }
            };

            factory.getMarshallerFactory().registerMarshaller(AuthnRequest.DEFAULT_ELEMENT_NAME, marshaller);
        });
    }

    private LazyInMemoryRelyingPartyRegistrationRepository relyingPartyRegistrationRepository;

    public OpenLSamlBuilder(PropertyResolver propertyResolver) {
        forceAuthN = Boolean.parseBoolean(propertyResolver.getProperty("security.saml.forceAuthN"));
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

    public OpenSaml4AuthenticationProvider openSaml4AuthenticationProvider() {
        return new OpenSaml4AuthenticationProvider();
    }

}
