package org.openl.spring.env;

import java.util.function.UnaryOperator;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.PropertyResolver;

@RequiredArgsConstructor
public class PropertyResolverFunction implements UnaryOperator<String> {

    private final PropertyResolver propertyResolver;

    @Override
    public String apply(String key) {
        return propertyResolver.getProperty(key);
    }
}
