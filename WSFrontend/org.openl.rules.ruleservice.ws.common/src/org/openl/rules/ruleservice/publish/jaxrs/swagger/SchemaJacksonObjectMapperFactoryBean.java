package org.openl.rules.ruleservice.publish.jaxrs.swagger;

import org.openl.rules.serialization.DefaultTypingMode;
import org.openl.rules.serialization.ProjectJacksonObjectMapperFactoryBean;

import com.fasterxml.jackson.annotation.JsonInclude;

public final class SchemaJacksonObjectMapperFactoryBean extends ProjectJacksonObjectMapperFactoryBean {

    @Override
    protected void applyAfterProjectConfiguration() {
        setGenerateSubtypeAnnotationsForDisabledMode(true);
        setSerializationInclusion(JsonInclude.Include.NON_NULL);
        setDefaultTypingMode(DefaultTypingMode.DISABLED);
        setDefaultDateFormat(null);
        setPolymorphicTypeValidation(false);
        setFailOnUnknownProperties(false);
    }
}
