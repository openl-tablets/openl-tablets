package org.openl.rules.ruleservice.publish.jaxrs.swagger.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.models.media.Schema;

public final class OpenApiObjectMapperConfigurationHelper {
    private OpenApiObjectMapperConfigurationHelper() {
    }

    public static void configure(ObjectMapper objectMapper) {
        objectMapper.addMixIn(Schema.class, OpenApiXmlIgnoreMixIn.class);
    }
}
