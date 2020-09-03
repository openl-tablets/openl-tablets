package org.openl.rules.ruleservice.publish.jaxrs.swagger.jackson;

import org.openl.rules.serialization.JacksonObjectMapperFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class OpenApiObjectMapperConfigurationFactoryBean extends AbstractFactoryBean<ObjectMapper> {

    private JacksonObjectMapperFactory jacksonObjectMapperFactory;

    public JacksonObjectMapperFactory getJacksonObjectMapperFactory() {
        return jacksonObjectMapperFactory;
    }

    public void setJacksonObjectMapperFactory(JacksonObjectMapperFactory jacksonObjectMapperFactory) {
        this.jacksonObjectMapperFactory = jacksonObjectMapperFactory;
    }

    @Override
    public Class<?> getObjectType() {
        return ObjectMapper.class;
    }

    @Override
    protected ObjectMapper createInstance() throws Exception {
        ObjectMapper objectMapper = getJacksonObjectMapperFactory().createJacksonObjectMapper();
        return OpenApiObjectMapperConfigurationHelper.configure(objectMapper);
    }
}
