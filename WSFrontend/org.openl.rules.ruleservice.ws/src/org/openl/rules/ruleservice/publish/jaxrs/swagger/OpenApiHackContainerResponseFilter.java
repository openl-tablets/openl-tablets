package org.openl.rules.ruleservice.publish.jaxrs.swagger;

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
