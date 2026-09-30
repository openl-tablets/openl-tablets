package org.openl.rules.spring.openapi.model;

import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SecuritySchemePair {

    @Getter
    public final String key;
    @Getter
    public final SecurityScheme securityScheme;
}
