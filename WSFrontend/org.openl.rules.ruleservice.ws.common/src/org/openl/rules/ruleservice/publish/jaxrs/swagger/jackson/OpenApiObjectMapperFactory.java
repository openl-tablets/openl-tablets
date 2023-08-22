package org.openl.rules.ruleservice.publish.jaxrs.swagger.jackson;

import org.openl.rules.serialization.ObjectMapperFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.integration.IntegrationObjectMapperFactory;

public final class OpenApiObjectMapperFactory implements ObjectMapperFactory {
    @Override
    public ObjectMapper createObjectMapper() {
        return IntegrationObjectMapperFactory.createJson();
    }
}
