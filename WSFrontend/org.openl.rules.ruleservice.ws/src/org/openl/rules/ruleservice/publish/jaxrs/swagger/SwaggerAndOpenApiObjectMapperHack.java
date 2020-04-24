package org.openl.rules.ruleservice.publish.jaxrs.swagger;

import com.fasterxml.jackson.databind.ObjectMapper;

public interface SwaggerAndOpenApiObjectMapperHack {
    void apply(ObjectMapper objectMapper);

    void revert();
}
