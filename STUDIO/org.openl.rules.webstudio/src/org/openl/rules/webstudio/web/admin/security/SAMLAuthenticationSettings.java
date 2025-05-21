package org.openl.rules.webstudio.web.admin.security;

import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.Parameter;

import org.openl.config.PropertiesHolder;
import org.openl.rules.rest.settings.model.validation.CertificateConstraint;
import org.openl.rules.webstudio.web.admin.SettingPropertyName;
import org.openl.rules.webstudio.web.install.KeyPairCertUtils;

public class SAMLAuthenticationSettings extends InheritedAuthenticationSettings {

    private static final String ENTITY_ID = "security.saml.entity-id";
    private static final String METADATA_URL = "security.saml.saml-server-metadata-url";
    private static final String BINDING = "security.saml.binding";
    private static final String SERVER_CERTIFICATE = "security.saml.server-certificate";
    private static final String FORCE_AUTHN = "security.saml.forceAuthN";
    private static final String LOCAL_KEY = "security.saml.local-key";
    private static final String LOCAL_CERTIFICATE = "security.saml.local-certificate";

    @Parameter(description = "SAML Entity ID. Must match the client ID or SP entity ID in the IdP.")
    @SettingPropertyName(ENTITY_ID)
    @NotBlank
    private String entityId;

    @Parameter(description = "SAML server metadata URL (XML format) used for autoconfiguration.")
    @SettingPropertyName(METADATA_URL)
    @NotBlank
    private String metadataUrl;

    @Parameter(description = "SAML binding method (e.g., HTTP-Redirect, HTTP-POST).")
    @SettingPropertyName(BINDING)
    private String binding;

    @Parameter(description = "Base64-encoded public key of the SAML IdP server.")
    @SettingPropertyName(SERVER_CERTIFICATE)
    @CertificateConstraint
    private String serverCertificate;

    @Parameter(description = "If true, IdP forces the user to reauthenticate.")
    @SettingPropertyName(FORCE_AUTHN)
    private boolean forceAuthN;

    @Parameter(description = "SAML attributes settings.")
    @Valid
    @NotNull
    private SAMLAttributesSettings attributes;

    public SAMLAuthenticationSettings() {
        attributes = new SAMLAttributesSettings();
    }

    @Override
    public void load(PropertiesHolder properties) {
        super.load(properties);
        entityId = properties.getProperty(ENTITY_ID);
        metadataUrl = properties.getProperty(METADATA_URL);
        binding = properties.getProperty(BINDING);
        serverCertificate = properties.getProperty(SERVER_CERTIFICATE);
        forceAuthN = Optional.ofNullable(properties.getProperty(FORCE_AUTHN))
                .map(Boolean::parseBoolean)
                .orElse(false);
        attributes.load(properties);
    }

    @Override
    public void store(PropertiesHolder properties) {
        super.store(properties);
        properties.setProperty(ENTITY_ID, entityId);
        properties.setProperty(METADATA_URL, metadataUrl);
        properties.setProperty(BINDING, binding);
        properties.setProperty(SERVER_CERTIFICATE, serverCertificate);
        properties.setProperty(FORCE_AUTHN, forceAuthN);
        attributes.store(properties);

        if (properties.getProperty(LOCAL_KEY) == null || properties.getProperty(LOCAL_CERTIFICATE) == null) {
            var pair = KeyPairCertUtils.generateCertificate();
            if (pair != null) {
                properties.setProperty(LOCAL_KEY, pair.getKey());
                properties.setProperty(LOCAL_CERTIFICATE, pair.getValue());
            }
        }
    }

    @Override
    public void revert(PropertiesHolder properties) {
        properties.revertProperties(
                ENTITY_ID,
                METADATA_URL,
                BINDING,
                SERVER_CERTIFICATE,
                FORCE_AUTHN,
                LOCAL_KEY,
                LOCAL_CERTIFICATE
        );
        attributes.revert(properties);
        super.revert(properties);
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getMetadataUrl() {
        return metadataUrl;
    }

    public void setMetadataUrl(String metadataUrl) {
        this.metadataUrl = metadataUrl;
    }

    public String getBinding() {
        return binding;
    }

    public void setBinding(String binding) {
        this.binding = binding;
    }

    public String getServerCertificate() {
        return serverCertificate;
    }

    public void setServerCertificate(String serverCertificate) {
        this.serverCertificate = serverCertificate;
    }

    public boolean isForceAuthN() {
        return forceAuthN;
    }

    public void setForceAuthN(boolean forceAuthN) {
        this.forceAuthN = forceAuthN;
    }

    public SAMLAttributesSettings getAttributes() {
        return attributes;
    }

    public void setAttributes(SAMLAttributesSettings attributes) {
        this.attributes = attributes;
    }
}

