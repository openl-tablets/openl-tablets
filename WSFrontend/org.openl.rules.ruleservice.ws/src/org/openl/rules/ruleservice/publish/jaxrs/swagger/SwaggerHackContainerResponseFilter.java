package org.openl.rules.ruleservice.publish.jaxrs.swagger;

import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;

@PreMatching
@Priority(Integer.MIN_VALUE)
public class SwaggerHackContainerResponseFilter implements ContainerResponseFilter {
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        Object lock = requestContext.getProperty("SwaggerHackContainerRequestFilterLock");
        if (lock instanceof ReentrantLock) {
            ReentrantLock reentrantLock = (ReentrantLock) lock;
            reentrantLock.unlock();
            requestContext.removeProperty("SwaggerHackContainerRequestFilterLock");
        }
        Object swaggerObjectMapperHack = requestContext.getProperty("SwaggerObjectMapperHack");
        if (swaggerObjectMapperHack instanceof SwaggerObjectMapperHack) {
            ((SwaggerObjectMapperHack) swaggerObjectMapperHack).revert();
            requestContext.removeProperty("SwaggerObjectMapperHack");
        }
    }
}
