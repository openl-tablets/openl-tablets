package org.openl.security.saml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.openl.rules.security.Privilege;
import org.openl.rules.security.SimplePrivilege;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.UserExternalFlags;
import org.openl.util.StringUtils;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.PropertyResolver;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.OpenSamlAuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.util.CollectionUtils;

/**
 * Creates Saml2Authentication and SimpleUser based on the ResponseToken from the IDP.
 *
 * @author Eugene Biruk
 */
public class OpenLResponseAuthenticationConverter implements Converter<OpenSamlAuthenticationProvider.ResponseToken, Saml2Authentication> {

    private final Function<SimpleUser, SimpleUser> authoritiesMapper;
    private final PropertyResolver propertyResolver;

    public OpenLResponseAuthenticationConverter(PropertyResolver propertyResolver,
                                                Function<SimpleUser, SimpleUser> authoritiesMapper) {
        this.propertyResolver = propertyResolver;
        this.authoritiesMapper = authoritiesMapper;
    }

    /**
     * Creates Saml2Authentication and SimpleUser based on the ResponseToken from the IDP.
     * @param responseToken ResponseToken from the IDP
     * @return Saml2Authentication
     */
    @Override
    public Saml2Authentication convert(OpenSamlAuthenticationProvider.ResponseToken responseToken) {
        Assertion assertion = CollectionUtils.firstElement(responseToken.getResponse().getAssertions());
        String username = assertion.getSubject().getNameID().getValue();
        SimpleUserSamlBuilder simpleUserBuilder = new SimpleUserSamlBuilder(propertyResolver);
        simpleUserBuilder.setAssertionAttributes(assertion);
        SimpleUser simpleUser = simpleUserBuilder.build();
        simpleUser.setUsername(username);
        simpleUser = authoritiesMapper.apply(simpleUser);

        List<Privilege> privileges = new ArrayList<>();
        privileges.addAll(simpleUser.getAuthorities());
        DefaultSaml2AuthenticatedPrincipal defaultSaml2AuthenticatedPrincipal = new DefaultSaml2AuthenticatedPrincipal(username, new HashMap<>());
        defaultSaml2AuthenticatedPrincipal.setRelyingPartyRegistrationId(responseToken.getToken().getRelyingPartyRegistration().getRegistrationId());
        return new Saml2Authentication(defaultSaml2AuthenticatedPrincipal,
            responseToken.getToken().getSaml2Response(), privileges);
    }

    /**
     * Builder is used to form a simpleUser based on the saml Assertion.
     *
     * @author Eugene Biruk
     */
    private class SimpleUserSamlBuilder {

        private final String usernameAttribute;
        private final String firstNameAttribute;
        private final String lastNameAttribute;
        private final String groupsAttribute;
        private final String emailAttribute;
        private final String displayNameAttribute;

        private final Map<String, List<String>> fields = new HashMap<>();

        public SimpleUserSamlBuilder(PropertyResolver propertyResolver) {
            this.usernameAttribute = propertyResolver.getProperty("security.saml.attribute.username");
            this.firstNameAttribute = propertyResolver.getProperty("security.saml.attribute.first-name");
            this.lastNameAttribute = propertyResolver.getProperty("security.saml.attribute.last-name");
            this.emailAttribute = propertyResolver.getProperty("security.saml.attribute.email");
            this.groupsAttribute = propertyResolver.getProperty("security.saml.attribute.groups");
            this.displayNameAttribute = propertyResolver.getProperty("security.saml.attribute.display-name");
            fields.put(usernameAttribute, new ArrayList<>());
            fields.put(firstNameAttribute, new ArrayList<>());
            fields.put(lastNameAttribute, new ArrayList<>());
            fields.put(emailAttribute, new ArrayList<>());
            fields.put(groupsAttribute, new ArrayList<>());
            fields.put(displayNameAttribute, new ArrayList<>());
        }

        private void setAssertionAttributes(Assertion assertion) {
            for (AttributeStatement attributeStatement : assertion.getAttributeStatements()) {
                for (Attribute attribute : attributeStatement.getAttributes()) {
                    if (fields.containsKey(attribute.getName())) {
                        List<String> attributeValues = new ArrayList<>();
                        for (XMLObject xmlObject : attribute.getAttributeValues()) {
                            String attributeValue = getXmlObjectValue(xmlObject);
                            if (attributeValue != null) {
                                attributeValues.add(attributeValue);
                            }
                        }
                        fields.get(attribute.getName()).addAll(attributeValues);
                    }
                }
            }
        }

        public SimpleUser build() {
            final List<Privilege> grantedAuthorities = new ArrayList<>();
            if (StringUtils.isNotBlank(groupsAttribute)) {
                String[] names = getAttributeAsStringArray(groupsAttribute);
                if (names != null) {
                    for (final String name : names) {
                        grantedAuthorities.add(new SimplePrivilege(name, name));
                    }
                }
            }

            UserExternalFlags externalFlags = UserExternalFlags.builder()
                .applyFeature(UserExternalFlags.Feature.EXTERNAL_FIRST_NAME,
                    StringUtils.isNotBlank(getAttributeAsString(firstNameAttribute)))
                .applyFeature(UserExternalFlags.Feature.EXTERNAL_LAST_NAME,
                    StringUtils.isNotBlank(getAttributeAsString(lastNameAttribute)))
                .applyFeature(UserExternalFlags.Feature.EXTERNAL_EMAIL,
                    StringUtils.isNotBlank(getAttributeAsString(emailAttribute)))
                .applyFeature(UserExternalFlags.Feature.EXTERNAL_DISPLAY_NAME,
                    StringUtils.isNotBlank(getAttributeAsString(displayNameAttribute)))
                .withFeature(UserExternalFlags.Feature.SYNC_EXTERNAL_GROUPS)
                .build();

            return new SimpleUser(getAttributeAsString(firstNameAttribute),
                getAttributeAsString(lastNameAttribute),
                getAttributeAsString(usernameAttribute),
                null,
                grantedAuthorities,
                getAttributeAsString(emailAttribute),
                getAttributeAsString(displayNameAttribute),
                externalFlags);
        }

        private String getAttributeAsString(String key) {
            List<String> values = fields.get(key);
            return CollectionUtils.firstElement(values);
        }

        private String[] getAttributeAsStringArray(String key) {
            List<String> values = fields.get(key);
            if (!CollectionUtils.isEmpty(values)) {
                return values.toArray(new String[0]);
            }
            return null;
        }

        //The resulting fields are used to create a SimpleUser, only strings are expected.
        private String getXmlObjectValue(XMLObject xmlObject) {
            if (xmlObject instanceof XSString) {
                return ((XSString) xmlObject).getValue();
            }
            return null;
        }

    }
}
