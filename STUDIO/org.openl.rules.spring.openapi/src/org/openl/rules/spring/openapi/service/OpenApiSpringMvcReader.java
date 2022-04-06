package org.openl.rules.spring.openapi.service;

import java.util.Map;

/**
 * Open API reader for Spring MVC
 *
 * @author Vladyslav Pikus
 */
public interface OpenApiSpringMvcReader {

    /**
     * Read OpenAPI schema for controllers from list
     *
     * @param openApiContext current OpenAPI context
     * @param controllers included Spring Controllers
     */
    void read(OpenApiContext openApiContext, Map<String, Class<?>> controllers);

}
