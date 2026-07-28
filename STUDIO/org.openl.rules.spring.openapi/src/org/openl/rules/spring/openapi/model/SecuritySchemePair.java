package org.openl.rules.spring.openapi.model;

import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SecuritySchemePair {

    public final String key;
    public final SecurityScheme securityScheme;

    public String getKey() {
        return key;
    }

    public SecurityScheme getSecurityScheme() {
        return securityScheme;
    }
}
