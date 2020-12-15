package org.openl.rules.ruleservice.publish.jaxrs.swagger.jackson;

import org.springframework.beans.factory.InitializingBean;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.models.AbstractModel;
import io.swagger.models.properties.Property;
import io.swagger.util.Json;

public final class SwaggerObjectMapperConfigurationBean implements InitializingBean {

    @Override
    public void afterPropertiesSet() {
        ObjectMapper objectMapper = Json.mapper();
        objectMapper.addMixIn(AbstractModel.class, SwaggerXmlIgnoreMixIn.class);
        objectMapper.addMixIn(Property.class, SwaggerXmlIgnoreMixIn.class);
    }
}
