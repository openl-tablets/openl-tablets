package org.openl.rules.spring.openapi.service;

import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * Open API property value resolver
 *
 * @author Vladyslav Pikus
 */
@Component
public class OpenApiPropertyResolver {

    private final MessageSource apiMessageSource;

    @Autowired
    public OpenApiPropertyResolver(@Qualifier("openApiMessageSource") Optional<MessageSource> apiMessageSource) {
        this.apiMessageSource = apiMessageSource.orElse(null);
    }

    public String resolve(String propertyValue) {
        if (apiMessageSource == null) {
            return propertyValue;
        }
        return apiMessageSource.getMessage(propertyValue, null, propertyValue, Locale.ENGLISH);
    }
}
