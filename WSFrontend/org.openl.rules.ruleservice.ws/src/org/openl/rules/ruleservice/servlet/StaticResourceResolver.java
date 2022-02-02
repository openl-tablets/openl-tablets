package org.openl.rules.ruleservice.servlet;

import org.apache.cxf.resource.ResourceResolver;

import java.io.InputStream;

class StaticResourceResolver implements ResourceResolver {

    private final static String PREFIX = "static";

    @Override
    public <T> T resolve(String resourceName, Class<T> resourceType) {
        InputStream stream = getAsStream(resourceName);
        if (resourceType.isInstance(stream)) {
            return resourceType.cast(stream);
        }
        return null;
    }

    @Override
    public InputStream getAsStream(String name) {
        return getClass().getClassLoader().getResourceAsStream(PREFIX + name);
    }

    public static boolean isResourceExists(String name) {
        return StaticResourceResolver.class.getClassLoader().getResource(PREFIX + name) != null;
    }
}
