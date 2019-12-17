package org.openl.spring.env;

import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.StreamSupport;

import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertyResolver;

public class PropertiesProvider {

    private PropertyResolver propertyResolver;

    public PropertiesProvider(PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
    }

    @SuppressWarnings("rawtypes")
    public HashMap<String, Object> getSystemProperties() {
        HashMap<String, Object> result = new HashMap<>();
        MutablePropertySources propSrcs = ((AbstractEnvironment) propertyResolver).getPropertySources();
        StreamSupport.stream(propSrcs.spliterator(), false)
            .filter(ps -> ps instanceof EnumerablePropertySource)
            .map(ps -> ((EnumerablePropertySource) ps).getPropertyNames())
            .flatMap(Arrays::stream)
            .filter(x -> propertyResolver.containsProperty(x))
            .forEach(propName -> result.put(propName, propertyResolver.getProperty(propName)));
        return result;
    }
}
