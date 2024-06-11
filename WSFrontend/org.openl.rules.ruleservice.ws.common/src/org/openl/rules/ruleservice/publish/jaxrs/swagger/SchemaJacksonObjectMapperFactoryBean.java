package org.openl.rules.ruleservice.publish.jaxrs.swagger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.openl.rules.ruleservice.publish.jaxrs.swagger.jackson.OpenApiObjectMapperFactory;
import org.openl.rules.serialization.JacksonObjectMapperFactoryBean;
import org.openl.rules.serialization.ProjectJacksonObjectMapperFactoryBean;

public final class SchemaJacksonObjectMapperFactoryBean extends ProjectJacksonObjectMapperFactoryBean {

    protected ObjectMapper enhanceObjectMapper(ObjectMapper objectMapper) {
        ObjectMapper ret = super.enhanceObjectMapper(objectMapper);
        ret.deactivateDefaultTyping();
        return ret;
    }

    @Override
    protected void applyAfterProjectConfiguration() {
        getDelegate().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        getDelegate().setPolymorphicTypeValidation(false);
        getDelegate().setFailOnUnknownProperties(false);
        getDelegate().setDefaultDateFormat(JacksonObjectMapperFactoryBean.getISO8601Format());
        getDelegate().setCaseInsensitiveProperties(false);
        getDelegate().setFailOnEmptyBeans(true);
        getDelegate().setObjectMapperFactory(new OpenApiObjectMapperFactory());
    }
}
