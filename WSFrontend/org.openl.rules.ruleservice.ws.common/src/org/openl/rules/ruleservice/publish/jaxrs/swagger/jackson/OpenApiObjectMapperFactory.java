package org.openl.rules.ruleservice.publish.jaxrs.swagger.jackson;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.openl.rules.serialization.ObjectMapperFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class OpenApiObjectMapperFactory implements ObjectMapperFactory {
    @Override
    public ObjectMapper createObjectMapper() {
        try {
            Method createJsonMethod = io.swagger.v3.core.util.ObjectMapperFactory.class.getDeclaredMethod("createJson");
            createJsonMethod.setAccessible(true);
            return (ObjectMapper) createJsonMethod.invoke(null);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }
}
