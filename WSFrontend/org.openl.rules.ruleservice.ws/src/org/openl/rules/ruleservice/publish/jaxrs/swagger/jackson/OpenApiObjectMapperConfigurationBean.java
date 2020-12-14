package org.openl.rules.ruleservice.publish.jaxrs.swagger.jackson;

import org.springframework.beans.factory.InitializingBean;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;

public final class OpenApiObjectMapperConfigurationBean implements InitializingBean {

    @Override
    public void afterPropertiesSet() {
        Json.mapper().addMixIn(Schema.class, OpenApiXmlIgnoreMixIn.class);
    }
}
