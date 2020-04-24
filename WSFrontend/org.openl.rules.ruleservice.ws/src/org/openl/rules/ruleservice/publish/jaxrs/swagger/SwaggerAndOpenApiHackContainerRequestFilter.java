package org.openl.rules.ruleservice.publish.jaxrs.swagger;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.UriInfo;

import com.fasterxml.jackson.databind.ObjectMapper;

@PreMatching
@Priority(Priorities.HEADER_DECORATOR)
public class SwaggerAndOpenApiHackContainerRequestFilter implements ContainerRequestFilter {
    private static final String SWAGGER_LISTING_PATH_JSON = "swagger.json";
    private static final String SWAGGER_LISTING_PATH_YAML = "swagger.yaml";

    private static final String OPENAPI_LISTING_PATH_JSON = "openapi.json";
    private static final String OPENAPI_LISTING_PATH_YAML = "openapi.yaml";

    private final ObjectMapper objectMapper;

    private static final ReentrantLock lock = new ReentrantLock();

    public SwaggerAndOpenApiHackContainerRequestFilter(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
    }

    private SwaggerAndOpenApiObjectMapperHack getSwaggerAndOpenApiObjectMapperHack(UriInfo ui) {
        if (ui.getPath().endsWith(SWAGGER_LISTING_PATH_JSON) || ui.getPath().endsWith(SWAGGER_LISTING_PATH_YAML)) {
            return new SwaggerObjectMapperHack();
        } else if (ui.getPath().endsWith(OPENAPI_LISTING_PATH_JSON) || ui.getPath()
            .endsWith(OPENAPI_LISTING_PATH_YAML)) {
            return new OpenApiObjectMapperHack();
        } else {
            return null;
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        UriInfo ui = requestContext.getUriInfo();
        SwaggerAndOpenApiObjectMapperHack swaggerAndOpenApiObjectMapperHack = getSwaggerAndOpenApiObjectMapperHack(ui);
        if (swaggerAndOpenApiObjectMapperHack != null) {
            requestContext.setProperty("SwaggerAndOpenApiHackContainerRequestFilterLock", lock);
            lock.lock();
            OpenApiRulesCacheWorkaround.reset();
            swaggerAndOpenApiObjectMapperHack.apply(objectMapper);
            requestContext.setProperty("SwaggerAndOpenApiObjectMapperHack", swaggerAndOpenApiObjectMapperHack);
        }
    }
}
