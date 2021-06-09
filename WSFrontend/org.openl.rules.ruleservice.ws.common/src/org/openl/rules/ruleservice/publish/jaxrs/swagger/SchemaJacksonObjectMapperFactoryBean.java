package org.openl.rules.ruleservice.publish.jaxrs.swagger;

import org.openl.rules.serialization.ProjectJacksonObjectMapperFactoryBean;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class SchemaJacksonObjectMapperFactoryBean extends ProjectJacksonObjectMapperFactoryBean {

    protected ObjectMapper enhanceObjectMapper(ObjectMapper objectMapper) {
        ObjectMapper ret = super.enhanceObjectMapper(objectMapper);
        ret.deactivateDefaultTyping();
        return ret;
    }

    @Override
    protected void applyAfterProjectConfiguration() {
        setSerializationInclusion(JsonInclude.Include.NON_NULL);
        setDefaultDateFormat(null);
        setPolymorphicTypeValidation(false);
        setFailOnUnknownProperties(false);
    }
}
