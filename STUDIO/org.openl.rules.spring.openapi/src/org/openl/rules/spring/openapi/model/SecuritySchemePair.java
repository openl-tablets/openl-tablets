package org.openl.rules.spring.openapi.model;

import io.swagger.v3.oas.models.security.SecurityScheme;

public class SecuritySchemePair {

    public final String key;
    public final SecurityScheme securityScheme;

    public SecuritySchemePair(String key, SecurityScheme securityScheme) {
        this.key = key;
        this.securityScheme = securityScheme;
    }

    public String getKey() {
        return key;
    }

    public SecurityScheme getSecurityScheme() {
        return securityScheme;
    }
}
