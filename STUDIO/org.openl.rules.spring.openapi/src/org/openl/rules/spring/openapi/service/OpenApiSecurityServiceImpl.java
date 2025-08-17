package org.openl.rules.spring.openapi.service;

import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.openl.rules.spring.openapi.model.SecuritySchemePair;

/**
 * OpenAPI Security schema service
 *
 * @author Vladyslav Pikus
 */
@Component
public class OpenApiSecurityServiceImpl implements OpenApiSecurityService {

    private final SecuritySchemePair securitySchemePair;


    public OpenApiSecurityServiceImpl(@Value("${user.mode}") String userMode) {
        switch (userMode) {
            case "ad":
            case "multi":
                securitySchemePair = new SecuritySchemePair("basicAuth", new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic"));
                break;
            case "oauth2":
            case "saml":
            case "single":
            default:
                securitySchemePair = null;
                break;
        }
    }

    /**
     * Resolves security schema if present
     *
     * @param apiContext curren OpenAPI context
     */
    @Override
    public void generateGlobalSecurity(OpenApiContext apiContext) {
        if (securitySchemePair != null) {
            apiContext.getOpenAPI().addSecurityItem(new SecurityRequirement().addList(securitySchemePair.key));
            apiContext.getComponents().addSecuritySchemes(securitySchemePair.key, securitySchemePair.securityScheme);
        }
    }

}
