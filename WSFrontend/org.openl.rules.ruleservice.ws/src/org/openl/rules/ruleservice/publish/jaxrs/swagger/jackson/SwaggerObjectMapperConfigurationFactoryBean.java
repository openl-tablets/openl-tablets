package org.openl.rules.ruleservice.publish.jaxrs.swagger.jackson;

import org.openl.rules.serialization.JacksonObjectMapperFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.models.ModelImpl;
import io.swagger.models.properties.Property;

public final class SwaggerObjectMapperConfigurationFactoryBean extends AbstractFactoryBean<ObjectMapper> {

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
        objectMapper.addMixIn(ModelImpl.class, SwaggerXmlIgnoreMixIn.class);
        objectMapper.addMixIn(Property.class, SwaggerXmlIgnoreMixIn.class);
        return objectMapper;
    }
}
