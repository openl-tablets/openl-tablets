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
public class SwaggerHackContainerRequestFilter implements ContainerRequestFilter {
    private static final String APIDOCS_LISTING_PATH_JSON = "swagger.json";
    private static final String APIDOCS_LISTING_PATH_YAML = "swagger.yaml";

    private final ObjectMapper objectMapper;

    private static final ReentrantLock lock = new ReentrantLock();

    public SwaggerHackContainerRequestFilter(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        UriInfo ui = requestContext.getUriInfo();
        if (ui.getPath().endsWith(APIDOCS_LISTING_PATH_JSON) || ui.getPath().endsWith(APIDOCS_LISTING_PATH_YAML)) {
            requestContext.setProperty("SwaggerHackContainerRequestFilterLock", lock);
            lock.lock();
            SwaggerObjectMapperHack swaggerObjectMapperHack = new SwaggerObjectMapperHack();
            swaggerObjectMapperHack.apply(objectMapper);
            requestContext.setProperty("SwaggerObjectMapperHack", swaggerObjectMapperHack);
        }
    }
}
