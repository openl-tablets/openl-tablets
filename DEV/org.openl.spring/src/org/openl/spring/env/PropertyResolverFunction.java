package org.openl.spring.env;

import java.util.function.Function;

import org.springframework.core.env.PropertyResolver;

public class PropertyResolverFunction implements Function<String, String> {

    private final PropertyResolver propertyResolver;

    public PropertyResolverFunction(PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
    }

    @Override
    public String apply(String key) {
        return propertyResolver.getProperty(key);
    }
}
