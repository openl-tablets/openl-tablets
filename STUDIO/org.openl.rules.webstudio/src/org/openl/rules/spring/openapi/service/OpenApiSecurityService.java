package org.openl.rules.spring.openapi.service;

/**
 * OpenAPI Security schema service
 *
 * @author Vladyslav Pikus
 */
public interface OpenApiSecurityService {

    /**
     * Resolves security schema if present
     *
     * @param apiContext curren OpenAPI context
     */
    void generateGlobalSecurity(OpenApiContext apiContext);

}
