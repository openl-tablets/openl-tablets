package org.openl.rules.spring.openapi.conf;

import java.util.Collections;
import java.util.List;

import org.openl.rules.spring.openapi.SpringMvcHandlerMethodsHelper;
import org.openl.rules.spring.openapi.controller.OpenApiController;
import org.openl.rules.spring.openapi.service.OpenApiService;
import org.openl.rules.spring.openapi.service.OpenApiSpringMvcReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = { "org.openl.rules.spring.openapi.controller",
        "org.openl.rules.spring.openapi.service",
        "org.openl.rules.spring.openapi.converter" })
public class SpringMvcOpenApiConfiguration {

    @Bean
    public SpringMvcHandlerMethodsHelper springMvcHandlerMethodsHelper(ApplicationContext context) {
        return new SpringMvcHandlerMethodsHelper(context);
    }

    @Bean
    public OpenApiService openApiService(ApplicationContext context,
            SpringMvcHandlerMethodsHelper mvcHandlerMethodsHelper,
            OpenApiSpringMvcReader openApiSpringMvcReader) {
        return new OpenApiService(context,
            mvcHandlerMethodsHelper,
            openApiSpringMvcReader,
            Collections.singleton(OpenApiController.class));
    }

    @Bean("openLRestExceptionBasePackages")
    public List<String> openLRestExceptionBasePackages() {
        return Collections.singletonList("org.openl.rules.rest");
    }

}
