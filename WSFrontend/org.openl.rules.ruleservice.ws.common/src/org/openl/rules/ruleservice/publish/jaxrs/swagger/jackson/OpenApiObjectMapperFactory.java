package org.openl.rules.ruleservice.publish.jaxrs.swagger.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;

import org.openl.rules.serialization.ObjectMapperFactory;

public final class OpenApiObjectMapperFactory implements ObjectMapperFactory {
    @Override
    public ObjectMapper createObjectMapper() {
        return Json.mapper().copy();
    }
}
