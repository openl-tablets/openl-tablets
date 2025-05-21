package org.openl.rules.webstudio.web.admin.security;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.Parameter;

import org.openl.config.PropertiesHolder;
import org.openl.rules.webstudio.web.admin.SettingPropertyName;

public class CASAuthenticationSettings extends InheritedAuthenticationSettings {

    private static final String APP_URL = "security.cas.app-url";
    private static final String CAS_SERVER_URL_PREFIX = "security.cas.cas-server-url-prefix";

    @Parameter(description = "OpenL Studio server url.", example = "https://localhost:8443/webstudio")
    @SettingPropertyName(APP_URL)
    @NotBlank
    private String appUrl;

    @Parameter(description = "CAS server URL.", example = "https://localhost:9443/cas")
    @SettingPropertyName(CAS_SERVER_URL_PREFIX)
    @NotBlank
    private String casServerUrlPrefix;

    @Parameter(description = "CAS attributes settings.")
    @Valid
    @NotNull
    private CASAttributesSettings attributes;

    public CASAuthenticationSettings() {
        attributes = new CASAttributesSettings();
    }

    @Override
    public void load(PropertiesHolder properties) {
        super.load(properties);
        appUrl = properties.getProperty(APP_URL);
        casServerUrlPrefix = properties.getProperty(CAS_SERVER_URL_PREFIX);
        attributes.load(properties);
    }

    @Override
    public void store(PropertiesHolder properties) {
        super.store(properties);
        properties.setProperty(APP_URL, appUrl);
        properties.setProperty(CAS_SERVER_URL_PREFIX, casServerUrlPrefix);
        attributes.store(properties);
    }

    @Override
    public void revert(PropertiesHolder properties) {
        properties.revertProperties(APP_URL, CAS_SERVER_URL_PREFIX);
        attributes.revert(properties);
        super.revert(properties);
    }

    public String getAppUrl() {
        return appUrl;
    }

    public void setAppUrl(String appUrl) {
        this.appUrl = appUrl;
    }

    public String getCasServerUrlPrefix() {
        return casServerUrlPrefix;
    }

    public void setCasServerUrlPrefix(String casServerUrlPrefix) {
        this.casServerUrlPrefix = casServerUrlPrefix;
    }

    public CASAttributesSettings getAttributes() {
        return attributes;
    }

    public void setAttributes(CASAttributesSettings attributes) {
        this.attributes = attributes;
    }
}
