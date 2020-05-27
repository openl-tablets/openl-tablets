package org.openl.rules.ruleservice.publish.jaxrs.swagger;

import org.springframework.beans.factory.InitializingBean;

import io.swagger.v3.core.converter.ModelConverters;

public class OpenApiInitializationBean implements InitializingBean {
    private static boolean openApiInitialized = false;

    private synchronized static void initializeOpenApi() {
        if (!openApiInitialized) {
            ModelConverters.getInstance().addConverter(new OpenApiSupportConverter());
            openApiInitialized = true;
        }
    }

    @Override
    public void afterPropertiesSet() {
        initializeOpenApi();
    }
}
