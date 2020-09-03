package org.openl.rules.ruleservice.publish.jaxrs.swagger.jackson;

import org.openl.rules.serialization.ObjectMapperFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.util.Json;

public final class SwaggerObjectMapperFactory implements ObjectMapperFactory {
    @Override
    public ObjectMapper createObjectMapper() {
        return Json.mapper();
    }
}
