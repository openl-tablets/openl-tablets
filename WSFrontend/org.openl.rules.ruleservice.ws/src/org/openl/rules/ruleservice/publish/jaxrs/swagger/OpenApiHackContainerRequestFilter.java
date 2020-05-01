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
public final class OpenApiHackContainerRequestFilter implements ContainerRequestFilter {

    private static final String OPENAPI_LISTING_PATH_JSON = "openapi.json";
    private static final String OPENAPI_LISTING_PATH_YAML = "openapi.yaml";

    private final ObjectMapper objectMapper;

    private static final ReentrantLock lock = new ReentrantLock();

    public OpenApiHackContainerRequestFilter(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        UriInfo ui = requestContext.getUriInfo();
        if (ui.getPath().endsWith(OPENAPI_LISTING_PATH_JSON) || ui.getPath().endsWith(OPENAPI_LISTING_PATH_YAML)) {
            OpenApiObjectMapperHack openApiObjectMapperHack = new OpenApiObjectMapperHack();
            requestContext.setProperty("OpenApiHackContainerRequestFilterLock", lock);
            lock.lock();
            openApiObjectMapperHack.apply(objectMapper);
            OpenApiRulesCacheWorkaround.reset();
            requestContext.setProperty("OpenApiObjectMapperHack", openApiObjectMapperHack);
        }
    }
}
