package org.openl.spring.env;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;

public class PropertyResolverProvider {

    static ConfigurableEnvironment environment;

    public static String getProperty(String propertyName) {
        return environment.getProperty(propertyName);
    }

    public static Map<String, Object> getProperties() {
        HashMap<String, Object> result = new HashMap<>();
        environment.getPropertySources()
            .stream()
            .filter(ps -> ps instanceof EnumerablePropertySource)
            .map(ps -> ((EnumerablePropertySource) ps).getPropertyNames())
            .flatMap(Arrays::stream)
            .filter(environment::containsProperty)
            .forEach(propName -> result.put(propName, environment.getProperty(propName)));
        return result;
    }
}
