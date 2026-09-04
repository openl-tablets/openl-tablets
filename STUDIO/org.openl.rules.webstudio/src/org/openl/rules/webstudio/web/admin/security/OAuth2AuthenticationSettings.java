package org.openl.rules.webstudio.web.admin.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonMerge;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import org.openl.config.PropertiesHolder;
import org.openl.studio.settings.converter.SettingPropertyName;

@Schema(allOf = AuthenticationSettings.class)
public class OAuth2AuthenticationSettings extends InheritedAuthenticationSettings {

    private static final String CLIENT_ID = "security.oauth2.client-id";
    private static final String CLIENT_SECRET = "security.oauth2.client-secret";
    private static final String ISSUER_URI = "security.oauth2.issuer-uri";
    private static final String SCOPE = "security.oauth2.scope";
    private static final String GRANT_TYPE = "security.oauth2.grant-type";

    @Getter
    @Parameter(description = "OAuth2 client ID registered in the identity provider.")
    @Setter
    @SettingPropertyName(CLIENT_ID)
    @NotBlank
    private String clientId;

    @Getter
    @Parameter(description = "Client secret used for authenticating to the OAuth2 server.")
    @Setter
    @SettingPropertyName(value = CLIENT_SECRET, secret = true)
    @NotBlank
    private String clientSecret;

    @Getter
    @Parameter(description = "OAuth2 issuer URI (authorization server metadata URL).")
    @Setter
    @SettingPropertyName(ISSUER_URI)
    @NotBlank
    private String issuerUri;

    @Getter
    @Parameter(description = "Requested scopes for authorization (e.g., openid, profile, email).")
    @Setter
    @SettingPropertyName(SCOPE)
    @NotBlank
    private String scope;

    @Getter
    @Parameter(description = "OAuth2 grant type (e.g., authorization_code).")
    @Setter
    @SettingPropertyName(GRANT_TYPE)
    private String grantType;

    @Getter
    @Parameter(description = "OAuth2 attributes settings.")
    @Setter
    @Valid
    @NotNull
    @JsonMerge
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
}
