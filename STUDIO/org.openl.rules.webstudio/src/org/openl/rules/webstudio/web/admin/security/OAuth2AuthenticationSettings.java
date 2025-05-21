package org.openl.rules.webstudio.web.admin.security;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;

import org.openl.config.PropertiesHolder;
import org.openl.rules.webstudio.web.admin.SettingPropertyName;

public class OAuth2AuthenticationSettings extends InheritedAuthenticationSettings {

    private static final String CLIENT_ID = "security.oauth2.client-id";
    private static final String CLIENT_SECRET = "security.oauth2.client-secret";
    private static final String ISSUER_URI = "security.oauth2.issuer-uri";
    private static final String SCOPE = "security.oauth2.scope";
    private static final String GRANT_TYPE = "security.oauth2.grant-type";

    @Parameter(description = "OAuth2 client ID registered in the identity provider.")
    @SettingPropertyName(CLIENT_ID)
    @NotBlank
    private String clientId;

    @Parameter(description = "Client secret used for authenticating to the OAuth2 server.")
    @SettingPropertyName(CLIENT_SECRET)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank
    private String clientSecret;

    @Parameter(description = "OAuth2 issuer URI (authorization server metadata URL).")
    @SettingPropertyName(ISSUER_URI)
    @NotBlank
    private String issuerUri;

    @Parameter(description = "Requested scopes for authorization (e.g., openid, profile, email).")
    @SettingPropertyName(SCOPE)
    @NotBlank
    private String scope;

    @Parameter(description = "OAuth2 grant type (e.g., authorization_code).")
    @SettingPropertyName(GRANT_TYPE)
    private String grantType;

    @Parameter(description = "OAuth2 attributes settings.")
    @Valid
    @NotNull
    private OAuth2AttributesSettings attributes;

    public OAuth2AuthenticationSettings() {
        attributes = new OAuth2AttributesSettings();
    }

    @Override
    public void load(PropertiesHolder properties) {
        super.load(properties);
        clientId = properties.getProperty(CLIENT_ID);
        clientSecret = properties.getProperty(CLIENT_SECRET);
        issuerUri = properties.getProperty(ISSUER_URI);
        scope = properties.getProperty(SCOPE);
        grantType = properties.getProperty(GRANT_TYPE);
        attributes.load(properties);
    }

    @Override
    public void store(PropertiesHolder properties) {
        super.store(properties);
        properties.setProperty(CLIENT_ID, clientId);
        properties.setProperty(CLIENT_SECRET, clientSecret);
        properties.setProperty(ISSUER_URI, issuerUri);
        properties.setProperty(SCOPE, scope);
        properties.setProperty(GRANT_TYPE, grantType);
        attributes.store(properties);
    }

    @Override
    public void revert(PropertiesHolder properties) {
        properties.revertProperties(
                CLIENT_ID,
                CLIENT_SECRET,
                ISSUER_URI,
                SCOPE,
                GRANT_TYPE
        );
        attributes.revert(properties);
        super.revert(properties);
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getIssuerUri() {
        return issuerUri;
    }

    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public OAuth2AttributesSettings getAttributes() {
        return attributes;
    }

    public void setAttributes(OAuth2AttributesSettings attributes) {
        this.attributes = attributes;
    }
}
