package org.openl.rules.ruleservice.publish.jaxrs.swagger.jackson;

import org.openl.rules.serialization.ObjectMapperFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.core.util.Json;

public final class OpenApiObjectMapperFactory implements ObjectMapperFactory {
    @Override
    public ObjectMapper createObjectMapper() {
        // Be careful
        // It is not the same as OpenApiObjectMapperFactory
        return Json.mapper();
    }
}
