package org.openl.rules.ruleservice.publish.jaxrs.swagger;

import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;

@PreMatching
@Priority(Integer.MIN_VALUE)
public class SwaggerAndOpenApiHackContainerResponseFilter implements ContainerResponseFilter {
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        Object swaggerAndOpenApiObjectMapperHack = requestContext.getProperty("SwaggerAndOpenApiObjectMapperHack");
        if (swaggerAndOpenApiObjectMapperHack instanceof SwaggerAndOpenApiObjectMapperHack) {
            ((SwaggerAndOpenApiObjectMapperHack) swaggerAndOpenApiObjectMapperHack).revert();
            requestContext.removeProperty("SwaggerAndOpenApiObjectMapperHack");
        }

        Object lock = requestContext.getProperty("SwaggerAndOpenApiHackContainerRequestFilterLock");
        if (lock instanceof ReentrantLock) {
            ReentrantLock reentrantLock = (ReentrantLock) lock;
            reentrantLock.unlock();
            requestContext.removeProperty("SwaggerAndOpenApiHackContainerRequestFilterLock");
        }
    }
}
