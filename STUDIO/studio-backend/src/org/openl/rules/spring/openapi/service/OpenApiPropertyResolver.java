package org.openl.rules.spring.openapi.service;

/**
 * Open API property value resolver
 *
 * @author Vladyslav Pikus
 */
public interface OpenApiPropertyResolver {

    /**
     * Resolve code from message source
     *
     * @param propertyValue code
     * @return resolved message
     */
    String resolve(String propertyValue);

}
