package org.openl.security.saml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.openl.rules.security.Privilege;
import org.openl.rules.security.SimplePrivilege;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.User;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.PropertyResolver;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;

/**
 * Creates Saml2Authentication and SimpleUser based on the ResponseToken from the IDP.
 *
 * @author Eugene Biruk
 */
public class OpenLResponseAuthenticationConverter implements Converter<OpenSaml4AuthenticationProvider.ResponseToken, Saml2Authentication> {

    private final BiFunction<String, Collection<? extends GrantedAuthority>, Collection<Privilege>> privilegeMapper;
    private final Consumer<User> syncUserData;
    private final PropertyResolver propertyResolver;

    public OpenLResponseAuthenticationConverter(PropertyResolver propertyResolver,
             Consumer<User> syncUserData,
             BiFunction<String, Collection<? extends GrantedAuthority>, Collection<Privilege>> privilegeMapper) {
        this.propertyResolver = propertyResolver;
        this.syncUserData = syncUserData;
        this.privilegeMapper = privilegeMapper;
    }

    /**
     * Creates Saml2Authentication and SimpleUser based on the ResponseToken from the IDP.
     * 
     * @param responseToken ResponseToken from the IDP
     * @return Saml2Authentication
     */
    @Override
    public Saml2Authentication convert(OpenSaml4AuthenticationProvider.ResponseToken responseToken) {
        Assertion assertion = responseToken.getResponse().getAssertions().iterator().next();
        SimpleUserSamlBuilder simpleUserBuilder = new SimpleUserSamlBuilder(propertyResolver);
        simpleUserBuilder.setAssertionAttributes(assertion);
        simpleUserBuilder.setNameID(assertion.getSubject().getNameID().getValue());
        SimpleUser simpleUser = simpleUserBuilder.build();

        syncUserData.accept(simpleUser);

        Collection<Privilege> privileges = privilegeMapper.apply(simpleUser.getUsername(), simpleUser.getAuthorities());

        DefaultSaml2AuthenticatedPrincipal principal = new DefaultSaml2AuthenticatedPrincipal(simpleUser.getUsername(), Collections.emptyMap());
        principal.setRelyingPartyRegistrationId(responseToken.getToken().getRelyingPartyRegistration().getRegistrationId());
        return new Saml2Authentication(principal, responseToken.getToken().getSaml2Response(), privileges);
    }

    /**
     * Builder is used to form a simpleUser based on the saml Assertion.
     *
     * @author Eugene Biruk
     */
    private static class SimpleUserSamlBuilder {

        private final String usernameAttribute;
        private final String firstNameAttribute;
        private final String lastNameAttribute;
        private final String groupsAttribute;
        private final String emailAttribute;
        private final String displayNameAttribute;

        private final Map<String, List<String>> fields = new HashMap<>();

        private String username;

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

        public void setNameID(String username) {
            this.username = username;
        }

        public SimpleUser build() {
            final List<Privilege> grantedAuthorities = new ArrayList<>();
            if (StringUtils.isNotBlank(groupsAttribute)) {
                for (String name : getAttributeValues(groupsAttribute)) {
                    grantedAuthorities.add(new SimplePrivilege(name));
                }
            }

            return SimpleUser.builder()
                .setFirstName(getAttributeAsString(firstNameAttribute))
                .setLastName(getAttributeAsString(lastNameAttribute))
                .setUsername(StringUtils.isBlank(usernameAttribute) ? username : getAttributeAsString(usernameAttribute))
                .setPrivileges(grantedAuthorities)
                .setEmail(getAttributeAsString(emailAttribute))
                .setDisplayName(getAttributeAsString(displayNameAttribute))
                .build();
        }

        private String getAttributeAsString(String key) {
            List<String> values = fields.get(key);
            return CollectionUtils.isNotEmpty(values) ? values.iterator().next() : null;
        }

        private List<String> getAttributeValues(String key) {
            return Collections.unmodifiableList(fields.getOrDefault(key, Collections.emptyList()));
        }

        // The resulting fields are used to create a SimpleUser, only strings are expected.
        private String getXmlObjectValue(XMLObject xmlObject) {
            if (xmlObject instanceof XSString) {
                return ((XSString) xmlObject).getValue();
            }
            return null;
        }

    }
}
