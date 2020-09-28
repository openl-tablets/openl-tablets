package org.openl.rules.ruleservice.publish.jaxrs.swagger;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;

@PreMatching
@Priority(Integer.MIN_VALUE)
public class OpenApiHackContainerResponseFilter implements ContainerResponseFilter {
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        List<String> contentType = requestContext.getHeaders().get("Content-Type");
        if (contentType.stream().filter(Objects::nonNull).noneMatch(e -> e.contains("charset"))) {
            requestContext.getHeaders().add("Content-Type", "charset=UTF-8");
        }
        Object openApiObjectMapperHack = requestContext.getProperty("OpenApiObjectMapperHack");
        if (openApiObjectMapperHack instanceof OpenApiObjectMapperHack) {
            ((OpenApiObjectMapperHack) openApiObjectMapperHack).revert();
            requestContext.removeProperty("OpenApiObjectMapperHack");
        }
        Object lock = requestContext.getProperty("OpenApiHackContainerRequestFilterLock");
        if (lock instanceof ReentrantLock) {
            ReentrantLock reentrantLock = (ReentrantLock) lock;
            reentrantLock.unlock();
            requestContext.removeProperty("OpenApiHackContainerRequestFilterLock");
        }
    }
}
