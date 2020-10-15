package org.openl.rules.ruleservice.publish.jaxrs.swagger.jackson;

import org.springframework.beans.factory.InitializingBean;

import io.swagger.v3.core.util.Json;

public final class OpenApiObjectMapperConfigurationBean implements InitializingBean {

    @Override
    public void afterPropertiesSet() {
        OpenApiObjectMapperConfigurationHelper.configure(Json.mapper());
    }
}
