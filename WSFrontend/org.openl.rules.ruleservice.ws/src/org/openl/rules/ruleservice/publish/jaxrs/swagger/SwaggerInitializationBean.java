package org.openl.rules.ruleservice.publish.jaxrs.swagger;

import org.springframework.beans.factory.InitializingBean;

import io.swagger.converter.ModelConverters;

public class SwaggerInitializationBean implements InitializingBean {

    private static boolean swaggerInitialized = false;

    private synchronized static void initializeSwagger() {
        if (!swaggerInitialized) {
            ModelConverters.getInstance().addConverter(new SwaggerSupportConverter());
            swaggerInitialized = true;
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        initializeSwagger();
    }

}
