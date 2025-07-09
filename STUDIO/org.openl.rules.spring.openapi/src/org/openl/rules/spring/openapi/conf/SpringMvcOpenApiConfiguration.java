package org.openl.rules.spring.openapi.conf;

import java.util.Collections;
import java.util.List;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import org.openl.rules.openapi.OpenAPIConfiguration;
import org.openl.rules.spring.openapi.SpringMvcHandlerMethodsHelper;
import org.openl.rules.spring.openapi.service.OpenApiServiceImpl;
import org.openl.rules.spring.openapi.service.OpenApiSpringMvcReader;

/**
 * Main Spring Configuration for Spring OpenAPI Generator
 */
@Configuration
@ComponentScan(basePackages = {"org.openl.rules.spring.openapi.controller",
        "org.openl.rules.spring.openapi.service",
        "org.openl.rules.spring.openapi.converter"})
public class SpringMvcOpenApiConfiguration {

    @Bean
    public SpringMvcHandlerMethodsHelper springMvcHandlerMethodsHelper(ApplicationContext context) {
        return new SpringMvcHandlerMethodsHelper(context);
    }

    @Bean
    public OpenApiServiceImpl openApiService(ApplicationContext context,
                                             OpenApiSpringMvcReader openApiSpringMvcReader) {
        OpenAPIConfiguration.configure();
        return new OpenApiServiceImpl(context, openApiSpringMvcReader);
    }

    @Bean("openLRestExceptionBasePackages")
    public List<String> openLRestExceptionBasePackages() {
        return Collections.singletonList("org.openl.rules.rest");
    }

}
