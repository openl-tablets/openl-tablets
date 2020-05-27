package org.openl.rules.ruleservice.publish.jaxrs.swagger.jackson;

import org.springframework.beans.factory.config.AbstractFactoryBean;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.models.media.Schema;

public final class OpenApiObjectMapperConfigurationFactoryBean extends AbstractFactoryBean<ObjectMapper> {

    private ObjectMapper objectMapper;

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Class<?> getObjectType() {
        return ObjectMapper.class;
    }

    @Override
    protected ObjectMapper createInstance() {
        ObjectMapper objectMapper = getObjectMapper();
        objectMapper.addMixIn(Schema.class, OpenApiXmlIgnoreMixIn.class);
        return objectMapper;
    }
}
