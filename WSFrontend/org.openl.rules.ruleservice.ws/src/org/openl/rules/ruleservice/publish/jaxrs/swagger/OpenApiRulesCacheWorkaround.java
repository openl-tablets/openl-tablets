package org.openl.rules.ruleservice.publish.jaxrs.swagger;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.models.Swagger;
import io.swagger.v3.oas.integration.OpenApiContextLocator;

public final class OpenApiRulesCacheWorkaround {

    private OpenApiRulesCacheWorkaround() {
    }

    @SuppressWarnings("unchecked")
    public static void reset() {
        try {
            Field mapField = OpenApiContextLocator.class.getDeclaredField("map");

            mapField.setAccessible(true);
            ConcurrentMap<String, Swagger> swaggerMap = (ConcurrentMap<String, Swagger>) mapField
                .get(OpenApiContextLocator.getInstance());
            swaggerMap.clear();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Logger log = LoggerFactory.getLogger(OpenApiRulesCacheWorkaround.class);
            log.error("Failed to reset OpenAPI cache.", e);
        }
    }
}
